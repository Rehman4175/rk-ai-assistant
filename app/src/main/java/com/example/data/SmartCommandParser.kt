package com.example.data

import java.util.regex.Pattern

object SmartCommandParser {

    enum class CommandType {
        REMINDER, TASK, EXPENSE, NOTE, DAIRY, HELP, UNKNOWN
    }

    data class ParsedCommand(
        val type: CommandType,
        val rawText: String,
        val commandWord: String,
        val restText: String
    )

    data class ReminderResult(val title: String, val dueTime: Long)

    fun parse(input: String): ParsedCommand {
        val trimmed = input.trim()
        val words = trimmed.split("\\s+".toRegex())
        val firstWord = words.firstOrNull()?.lowercase() ?: ""

        val commandType = when (firstWord) {
            "remind", "reminder" -> CommandType.REMINDER
            "task", "todo", "kaam", "kam", "work" -> CommandType.TASK
            "expense", "spent", "paisa", "kharcha" -> CommandType.EXPENSE
            "note", "addnote" -> CommandType.NOTE
            "dairy", "diary" -> CommandType.DAIRY
            "help", "?" -> CommandType.HELP
            else -> CommandType.UNKNOWN
        }

        return ParsedCommand(
            type = commandType,
            rawText = trimmed,
            commandWord = firstWord,
            restText = words.drop(1).joinToString(" ")
        )
    }

    /**
     * Robust Reminder Parser
     * Handles "remind me in 2 min", "2 min baad paani pina", etc.
     */
    fun parseReminder(input: String): ReminderResult? {
        val low = input.lowercase()
            .replace("reminder", "")
            .replace("remind", "")
            .replace("me", "")
            .replace("mujhe", "")
            .replace("add kro", "")
            .replace("add", "")
            .trim()

        if (low.isEmpty()) return null

        val now = System.currentTimeMillis()
        var delayMs: Long
        var cleanTitle = low

        // Time regex: handles "2 min", "1 hour", "30 sec"
        val timePattern = Pattern.compile("(\\d+)\\s*(min|minute|minutes|hr|hour|hours|sec|second|seconds|m|h|s)")
        val matcher = timePattern.matcher(low)
        
        if (matcher.find()) {
            val value = matcher.group(1)?.toLong() ?: 0L
            val unit = matcher.group(2) ?: "min"
            
            delayMs = when {
                unit.startsWith("h") -> value * 3600000L
                unit.startsWith("s") -> value * 1000L
                else -> value * 60000L
            }

            // Align to minute boundary (:00) if using minutes/hours for "theek" timing
            val triggerTime = if (!unit.startsWith("s")) {
                ((now / 60000) * 60000) + delayMs
            } else {
                now + delayMs
            }
            
            // Remove time part from title
            val timeString = matcher.group(0)
            cleanTitle = if (timeString != null) {
                low.replace(timeString, "")
                    .replace("baad", "")
                    .replace("baar", "")
                    .replace("in", "")
                    .trim()
            } else low
            
            val finalTitle = cleanTitle.ifBlank { "Reminder" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            return ReminderResult(finalTitle, triggerTime)
        } else {
            // Default 10 mins if no time found
            val triggerTime = ((now / 60000) * 60000) + (10 * 60000L)
            return ReminderResult("Reminder", triggerTime)
        }
    }
}
