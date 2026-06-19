package com.example.data

// ===== SMART COMMAND PARSER =====
// ✅ Sirf pehla word command decide karega

object SmartCommandParser {

    enum class CommandType {
        REMINDER,
        TASK,
        EXPENSE,
        EVENT,
        NOTE,
        DAIRY,
        SEARCH,
        HELP,
        UNKNOWN
    }

    data class ParsedCommand(
        val type: CommandType,
        val rawText: String,
        val commandWord: String,
        val restText: String,
        val delayMinutes: Int = 0,
        val delaySeconds: Int = 0,
        val delayHours: Int = 0,
        val delayDays: Int = 0
    )

    fun parse(input: String): ParsedCommand {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedCommand(CommandType.UNKNOWN, trimmed, "", "")
        }

        val words = trimmed.split("\\s+".toRegex())
        val firstWord = words.firstOrNull()?.lowercase() ?: ""

        // ✅ Pehla word command decide karega
        val commandType = when (firstWord) {
            "remind", "reminder" -> CommandType.REMINDER
            "task", "todo", "addtask" -> CommandType.TASK
            "expense", "spent", "pay" -> CommandType.EXPENSE
            "event", "meeting", "schedule" -> CommandType.EVENT
            "note", "addnote" -> CommandType.NOTE
            "dairy", "diary" -> CommandType.DAIRY
            "search", "find" -> CommandType.SEARCH
            "help", "?" -> CommandType.HELP
            else -> CommandType.UNKNOWN
        }

        val restText = words.drop(1).joinToString(" ")

        // ✅ Time extract karein (2 min, 30 sec, 1 hour, etc.)
        val (delayMinutes, delaySeconds, delayHours, delayDays) = extractTime(restText)

        return ParsedCommand(
            type = commandType,
            rawText = trimmed,
            commandWord = firstWord,
            restText = restText,
            delayMinutes = delayMinutes,
            delaySeconds = delaySeconds,
            delayHours = delayHours,
            delayDays = delayDays
        )
    }

    private fun extractTime(text: String): Tuple4 {
        var minutes = 0
        var seconds = 0
        var hours = 0
        var days = 0

        val regex = Regex("""(\d+)\s*(min|minute|minutes|sec|second|seconds|hour|hours|hr|day|days)""", RegexOption.IGNORE_CASE)
        regex.findAll(text).forEach { match ->
            val value = match.groupValues[1].toIntOrNull() ?: 0
            val unit = match.groupValues[2].lowercase()
            when {
                unit.startsWith("min") -> minutes = value
                unit.startsWith("sec") -> seconds = value
                unit.startsWith("hour") || unit.startsWith("hr") -> hours = value
                unit.startsWith("day") -> days = value
            }
        }

        return Tuple4(minutes, seconds, hours, days)
    }

    private data class Tuple4(val minutes: Int, val seconds: Int, val hours: Int, val days: Int)
}