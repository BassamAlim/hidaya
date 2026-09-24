package bassamalim.hidaya.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `deleteDirRecursive removes empty folders but keeps downloaded files`() {
        val root = tmp.newFolder("Telawat")
        val emptyNarration = File(root, "1/2").apply { mkdirs() }
        val downloaded = File(root, "3/4/0.mp3").apply { parentFile!!.mkdirs(); writeText("x") }

        FileUtils.deleteDirRecursive(root)

        assertFalse(emptyNarration.exists())
        assertFalse(File(root, "1").exists())
        assertTrue(downloaded.exists())
        assertTrue(root.exists())
    }

    @Test
    fun `deleteDirRecursive ignores a plain file`() {
        val file = tmp.newFile("a.mp3")

        FileUtils.deleteDirRecursive(file)

        assertTrue(file.exists())
    }

}
