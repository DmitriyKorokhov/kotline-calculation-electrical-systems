package ui.screens.shieldeditor

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

data class AccoreRunResult(
    val exitCode: Int,
    val output: String,
    val stagingDir: String?,
    val outDwg: String
)

object AutoCadExporter {

    // Поиск accoreconsole.exe в типичных местах установок Autodesk
    fun tryFindAccoreConsole(): String? {
        val envProgFiles = listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            System.getenv("ProgramW6432")
        ).distinct()

        val candidates = mutableListOf<String>()
        listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019").forEach { ver ->
            envProgFiles.forEach { base ->
                candidates.add("$base/Autodesk/AutoCAD $ver/accoreconsole.exe")
            }
        }
        // Частые «ручные» инсталляции
        candidates.add("C:/Program Files/Autodesk/AutoCAD 2022/accoreconsole.exe")

        return candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?.absolutePath
    }

    // Стаджинг только в %TEMP% согласно вашим предпочтениям
    private fun createTempStaging(): File {
        val base = File(System.getProperty("java.io.tmpdir"))
        val dir = File(base, "autoCadExport_${System.currentTimeMillis()}")
        dir.mkdirs()
        return dir
    }

    private fun toAcad(path: String): String = path.replace('\\', '/')

    // LISP: точная логика под Core Console — 2 аргумента, очистка Model Space, INSERT без диалогов, атрибут INFO
    private fun generateLispContent(): String = """
;; auto_runner.lsp — финальная версия для AcCoreConsole
;; - Очистка пространства модели через ssget/entdel.
;; - Вставка блоков командой "_.-INSERT" с масштабом/углом (1,1,0).
;; - ATTREQ=0, ATTDIA=0 на время вставок; затем восстановление.
;; - Заполнение атрибута INFO через entmod.
;; - Логирование OK/ERR по строкам CSV и DONE в конце.

(defun str-split (str delim / pos)
  (if (setq pos (vl-string-search delim str))
    (cons (substr str 1 pos) (str-split (substr str (+ pos 2)) delim))
    (list str)
  )
)

(defun clear-modelspace-all ( / ss i en)
  (setq ss (ssget "_X" '((410 . "Model"))))
  (if ss
    (progn
      (setq i 0)
      (while (< i (sslength ss))
        (setq en (ssname ss i))
        (if en (entdel en))
        (setq i (1+ i))
      )
    )
  )
  (princ)
)

(defun C:GENERATE-SCHEME (csv_path log_path /
  csv_file line parts log_file row block_name pt attr_text
  new_insert e a old_attreq old_attdia
)
  (princ "\n[LISP] Старт...")
  (setq log_file (open log_path "w"))
  (setq old_attreq (getvar "ATTREQ"))
  (setq old_attdia (getvar "ATTDIA"))
  (setvar "ATTREQ" 0)
  (setvar "ATTDIA" 0)

  (if log_file (write-line "INFO: clear modelspace" log_file))
  (clear-modelspace-all)

  (if (not (setq csv_file (open csv_path "r")))
    (progn
      (if log_file (write-line (strcat "ERR: cannot open CSV: " csv_path) log_file))
      (goto-cleanup)
    )
    (progn
      (setq row 1)
      (while (setq line (read-line csv_file))
        (setq parts (str-split line ";"))
        (if (and parts (>= (length parts) 4))
          (progn
            (setq block_name (nth 0 parts))
            (setq pt (list (distof (nth 1 parts)) (distof (nth 2 parts)) 0.0))
            ;; CSV хранит переносы как \n — превращаем в реальный перенос
            (setq attr_text (vl-string-subst "\n" "\\n" (nth 3 parts)))
            (if (tblsearch "BLOCK" block_name)
              (progn
                (command "_.-INSERT" block_name pt 1.0 1.0 0.0)
                (setq new_insert (entlast))
                (if (and new_insert (= "INSERT" (cdr (assoc 0 (entget new_insert)))))
                  (progn
                    (setq e (entnext new_insert))
                    (while (and e (= "ATTRIB" (cdr (assoc 0 (entget e)))))
                      (setq a (entget e))
                      (if (= "INFO" (cdr (assoc 2 a)))
                        (progn
                          (setq a (subst (cons 1 attr_text) (assoc 1 a) a))
                          (entmod a)
                        )
                      )
                      (setq e (entnext e))
                    )
                    (if log_file (write-line (strcat "OK row " (itoa row) ": " block_name) log_file))
                  )
                  (if log_file (write-line (strcat "ERR row " (itoa row) ": insert failed " block_name) log_file))
                )
              )
              (if log_file (write-line (strcat "ERR row " (itoa row) ": block not found " block_name) log_file))
            )
          )
          (if log_file (write-line (strcat "ERR row " (itoa row) ": bad csv format") log_file))
        )
        (setq row (1+ row))
      )
      (close csv_file)
    )
  )
  (goto-cleanup)
  (princ)
)

(defun goto-cleanup ( / )
  (if (boundp 'old_attreq) (setvar "ATTREQ" old_attreq))
  (if (boundp 'old_attdia) (setvar "ATTDIA" old_attdia))
  (if log_file (write-line "DONE" log_file))
  (if log_file (close log_file))
  (princ "\n[LISP] Готово.")
)
""".trimIndent()

    // SCR: загрузка LISP, вызов функции с 2 параметрами, SAVEAS 2018, QSAVE, QUIT
    private fun generateScrContent(
        lspAbs: String,
        csvAbs: String,
        logAbs: String,
        outDwgAbs: String
    ): String {
        return """
FILEDIA 0
(load "${toAcad(lspAbs)}")
(C:GENERATE-SCHEME "${toAcad(csvAbs)}" "${toAcad(logAbs)}")
_.SAVEAS
2018
${toAcad(outDwgAbs)}
_.QSAVE
_.QUIT
""".trimIndent()
    }

    /**
     * Основной экспорт:
     * - Открывает ваш templateDwgPath (должен содержать определения блоков) через ключ /i в AccoreConsole.
     * - Чистит модельное пространство LISP-ом и вставляет блоки по CSV, затем сохраняет в outDwgPath.
     */
    fun exportUsingTrustedStaging(
        accorePath: String?,
        templateDwgPath: String,
        outDwgPath: String,
        shieldData: ShieldData,
        baseX: Int = 0,
        stepX: Int = 50,
        y: Int = 0,
        timeoutSec: Long = 300L,
        useTemplateCopy: Boolean = false
    ): AccoreRunResult {
        val accore = accorePath?.let { File(it) } ?: tryFindAccoreConsole()?.let { File(it) }
        if (accore == null || !accore.exists()) {
            return AccoreRunResult(-1, "accoreconsole.exe not found. Provide path or install AutoCAD.", null, outDwgPath)
        }

        val template = File(templateDwgPath)
        if (!template.exists()) {
            return AccoreRunResult(-4, "Template DWG not found: $templateDwgPath", null, outDwgPath)
        }

        val staging = createTempStaging()

        val csvFile = File(staging, "shield_export_${System.currentTimeMillis()}.csv")
        val lspFile = File(staging, "auto_runner.lsp")
        val scrFile = File(staging, "run_script.scr")
        val lispLogFile = File(staging, "autocad_log.txt")

        // По желанию — копия шаблона в staging, иначе используем исходный путь
        val templateForAccore = if (useTemplateCopy) {
            try {
                val working = File(staging, "working_template.dwg")
                Files.copy(template.toPath(), working.toPath(), StandardCopyOption.REPLACE_EXISTING)
                working
            } catch (ex: Exception) {
                return AccoreRunResult(-8, "Cannot copy template into staging: ${ex.message}", staging.absolutePath, outDwgPath)
            }
        } else {
            template
        }

        // Экспорт CSV
        try {
            CsvExporter().export(shieldData, csvFile, baseX, stepX, y)
        } catch (ex: Exception) {
            return AccoreRunResult(-6, "Failed to write CSV: ${ex.message}", staging.absolutePath, outDwgPath)
        }

        // Генерация LISP и SCR (2 аргумента в вызове GENERATE-SCHEME)
        try {
            lspFile.writeText(generateLispContent(), StandardCharsets.UTF_8)
            scrFile.writeText(
                generateScrContent(
                    lspFile.absolutePath,
                    csvFile.absolutePath,
                    lispLogFile.absolutePath,
                    File(outDwgPath).absolutePath
                ),
                StandardCharsets.UTF_8
            )
        } catch (ex: Exception) {
            return AccoreRunResult(-7, "Failed to write LISP/SCR: ${ex.message}", staging.absolutePath, outDwgPath)
        }

        // Если целевой DWG уже существует — попробуем удалить
        val outDwgFile = File(outDwgPath)
        if (outDwgFile.exists()) {
            val ok = try { outDwgFile.delete() } catch (_: Exception) { false }
            if (!ok && outDwgFile.exists()) {
                return AccoreRunResult(-3, "Output DWG exists and is locked: ${outDwgFile.absolutePath}", staging.absolutePath, outDwgPath)
            }
        }

        // Запуск accoreconsole: открываем шаблон через /i, выполняем скрипт /s
        val cmd = listOf(
            accore.absolutePath,
            "/i", templateForAccore.absolutePath,
            "/s", scrFile.absolutePath
        )

        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        pb.directory(staging)

        val proc = try { pb.start() } catch (ex: Exception) {
            return AccoreRunResult(-9, "Failed to start accoreconsole: ${ex.message}", staging.absolutePath, outDwgPath)
        }

        val outBuilder = StringBuilder()
        val readerThread = Thread {
            try {
                proc.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                    lines.forEach { outBuilder.appendLine(it) }
                }
            } catch (_: Exception) { /* ignore */ }
        }
        readerThread.start()

        val completed = try { proc.waitFor(timeoutSec, TimeUnit.SECONDS) } catch (_: InterruptedException) { false }
        if (!completed) {
            proc.destroyForcibly()
            return AccoreRunResult(-2, "accoreconsole timed out after $timeoutSec seconds.\n$outBuilder", staging.absolutePath, outDwgPath)
        }

        readerThread.join(500)
        val exit = proc.exitValue()

        // Подождём появления LISP-лога (до 10s)
        val logAppeared = waitForFile(lispLogFile, 10_000)
        val lispLogText = if (logAppeared) {
            try { lispLogFile.readText(Charset.defaultCharset()) } catch (ex: Exception) { "Failed to read lisp log: ${ex.message}" }
        } else {
            ""
        }

        val accTemp = readLatestAccoreTempOutput()
        val combined = buildString {
            appendLine("ACCORERUN exit=$exit")
            appendLine("--- process stdout ---")
            appendLine(outBuilder.toString())
            appendLine("--- accore temp ---")
            appendLine(accTemp)
            appendLine("--- lisp_log (${lispLogFile.absolutePath}) ---")
            appendLine(lispLogText)
            appendLine("--- end logs ---")
        }

        if (exit == 0 && !outDwgFile.exists()) {
            return AccoreRunResult(exit, combined + "\n[WARN] accore exit=0 but output DWG not found: ${outDwgFile.absolutePath}", staging.absolutePath, outDwgPath)
        }

        return AccoreRunResult(exit, combined, staging.absolutePath, outDwgPath)
    }

    private fun waitForFile(file: File, timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (file.exists() && file.length() > 0) return true
            try { Thread.sleep(200) } catch (_: InterruptedException) { break }
        }
        return false
    }

    // Пытаемся прочитать «временный» лог Accore из %TEMP%, полезно при диагностике
    private fun readLatestAccoreTempOutput(): String {
        return try {
            val tmpDir = File(System.getProperty("java.io.tmpdir"))
            val candidates = tmpDir.listFiles { f ->
                f.isFile && (f.name.startsWith("acc") || f.name.startsWith("accc"))
            }?.toList() ?: return ""
            if (candidates.isEmpty()) return ""
            val latest = candidates.maxByOrNull { it.lastModified() } ?: return ""
            val bytes = latest.readBytes()
            val limit = 2 * 1024 * 1024
            val slice = if (bytes.size > limit) bytes.copyOf(limit) else bytes
            val decoders = listOf(
                Charset.forName("UTF-16LE"),
                StandardCharsets.UTF_8,
                Charset.forName("CP866")
            )
            for (cs in decoders) {
                try {
                    val s = String(slice, cs)
                    if (s.any { it.isLetterOrDigit() }) {
                        return "\n--- accore temp: ${latest.absolutePath} (${cs.displayName()}) ---\n$s\n--- end accore temp ---\n"
                    }
                } catch (_: Throwable) { }
            }
            val hex = slice.joinToString("") { String.format("%02X", it) }
            "\n--- accore temp (binary): ${latest.absolutePath} ---\n$hex\n--- end accore temp ---\n"
        } catch (ex: Exception) {
            "\n--- failed to read accore temp: ${ex.message} ---\n"
        }
    }
}
