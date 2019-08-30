package com.timmahh.ksoup

import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 *
 * @author Timothy Logan
 */
class TestParser {

    @Test
    fun testUG() {
        val file = File("src/test/", "test.html")
        print(file.absolutePath)
        val result = StudentUndergradInfo.builder.parse(FileInputStream(file), Charsets.UTF_8.name(), "https://mymadison.ps.jmu.edu/")
        print(result)
    }

}
data class Subject(val name: String = "", val gpa: Float = 0f)

data class StudentUndergradInfo(
    var holds: Int = 0,
    var toDos: Int = 0,
    var cumGPA: Float = 0f,
    var lastSemGPA: Float = 0f,
    var hoursEnrolled: MutableMap<String, Int> = mutableMapOf(),
    val majors: MutableList<Subject> = mutableListOf(),
    val minors: MutableList<Subject> = mutableListOf(),
    var gpaLastUpdated: Date = Calendar.getInstance().time
) {
    constructor() : this(0)


    companion object {
        val builder: SimpleParser<StudentUndergradInfo> by buildParser(::StudentUndergradInfo) {
            StudentUndergradInfo::holds.int("div#SSSGROUPBOXRIGHTNBO2_WRAPPER span.PSHYPERLINKDISABLED")
            StudentUndergradInfo::toDos.int("div#SSSGROUPBOXRIGHTNBO3_WRAPPER span.PSHYPERLINKDISABLED")
            StudentUndergradInfo::cumGPA.float("div#SSSGROUPBOXRIGHTNBO4_WRAPPER span.PSHYPERLINKDISABLED")
            StudentUndergradInfo::lastSemGPA.float("div#SSSGROUPBOXRIGHTNBO5_WRAPPER span.PSHYPERLINKDISABLED")
            text("div#SSSGROUPBOXRIGHTNBO6_WRAPPER > span") { text, value ->
                value.hoursEnrolled.putAll(
                    text.substringBefore("Hours Enrolled")
                        .replace("\\s".toRegex(), "")
                        .split(":".toRegex())
                        .chunked(2) { (name, amount) -> name to amount.toInt() }
                )
            }
            text("div#SSSGROUPBOXRIGHTNBO7_WRAPPER > span") { text, value ->
                fun Map<String, List<String>>.getSubjects(key: String) =
                    getOrElse(key, ::emptyList).map {
                        Subject(it.substringBefore(" ("), it.substringAfter("GPA ").toFloatOrNull() ?: 0f)
                    }
                with(text.trim().split("\\)\\s".toRegex()).filter(String::isNotBlank)
                    .groupBy({ it.substringBefore(":") }) { it.substringAfter(": ") }) {
                    value.majors += getSubjects("Major")
                    value.minors += getSubjects("Minor")
                    value.gpaLastUpdated = SimpleDateFormat("MM/dd/yyyy").parse(
                        getOrElse("Major/Minor GPA Last Updated", ::emptyList).firstOrNull() ?: "00/00/0000"
                    )
                }
            }
        }
    }
}


data class DeclaredSubject(
    val majors: List<Subject> = emptyList(),
    val minors: List<Subject> = emptyList(),
    val gpaLastUpdated: Date = Calendar.getInstance().time
)


object UGInfo : ParseBuilder<StudentUndergradInfo>() {

    override val build: SimpleParser<StudentUndergradInfo> by buildParser(::StudentUndergradInfo) {

        /*"div#SSSGROUPBOXRIGHTNBO2_WRAPPER span.PSHYPERLINKDISABLED".int(StudentUndergradInfo::holds)
        "div#SSSGROUPBOXRIGHTNBO3_WRAPPER span.PSHYPERLINKDISABLED".int(StudentUndergradInfo::toDos)
        "div#SSSGROUPBOXRIGHTNBO4_WRAPPER span.PSHYPERLINKDISABLED".float(StudentUndergradInfo::cumGPA)
        "div#SSSGROUPBOXRIGHTNBO5_WRAPPER span.PSHYPERLINKDISABLED".float(StudentUndergradInfo::lastSemGPA)
        "div#SSSGROUPBOXRIGHTNBO6_WRAPPER > span".text() { text, value ->
            value.hoursEnrolled + with(text.replace("\\s".toRegex(), "")) {
                        substring(0, indexOf("Hours Enrolled"))
                    }.split(Regex(": (?:\\s+)")).filter(String::isNotBlank)
                        .chunked(2) { (name, amount) -> name to amount.toInt() }
                        .toMap()
        }
        "div#SSSGROUPBOXRIGHTNBO7_WRAPPER > span".text { text, value: StudentUndergradInfo ->
            value.subject = with(text.trim()
                .split(Regex("\\)\\s"))
                .filter(String::isNotBlank)
                .groupBy({ it.substringBefore(":") }) { it.substringAfter(": ") }) {

                DeclaredSubject(
                    this.getOrDefault("Major", emptyList()).map {
                        Subject(
                            it.substring(0, it.indexOf(" (")),
                            it.substringAfter("GPA ").toFloat()
                        )
                    },
                    this.getOrDefault("Minor", emptyList()).map {
                        Subject(
                            it.substring(0, it.indexOf(" (")),
                            it.substringAfter("GPA ").toFloat()
                        )
                    },
                    SimpleDateFormat("MM/dd/yyyy").parse(
                        this.getOrDefault(
                            "Major/Minor GPA Last Updated",
                            emptyList()
                        ).firstOrNull() ?: "00/00/0000"
                    )
                )
            }
        }*/
    }
}
