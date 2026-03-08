package com.GenStack;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GenStackTest
{
    /**
     * @class RecordingPrintStream
     * @brief Test-only PrintStream stub that records print/println calls.
     *
     * This stub collects printed lines into a list for assertions instead of writing to the console.
     */
    static class RecordingPrintStream extends PrintStream {
        private final List<String> lines = new ArrayList<>();
        private final StringBuilder partial = new StringBuilder();

        /**
         * Construct a RecordingPrintStream (no underlying output).
         */
        RecordingPrintStream() {
            super(new OutputStream() { public void write(int b) { /* no-op */ } });
        }

        @Override
        public void println(String x) {
            // flush any partial before adding full line
            if (partial.length() > 0) {
                lines.add(partial.toString());
                partial.setLength(0);
            }
            lines.add(x);
        }

        @Override
        public void println() {
            if (partial.length() > 0) {
                lines.add(partial.toString());
                partial.setLength(0);
            } else {
                lines.add("");
            }
        }

        @Override
        public void print(String s) {
            // accumulate partial prints (e.g., prompts) and record as a line
            partial.append(s);
            // record prompt-like prints immediately
            lines.add(partial.toString());
            partial.setLength(0);
        }

        /**
         * @brief Return all recorded lines.
         * @return list of lines printed during the test
         */
        List<String> getLines() {
            // if a partial remains, include it
            if (partial.length() > 0) {
                List<String> copy = new ArrayList<>(lines);
                copy.add(partial.toString());
                return copy;
            }
            return new ArrayList<>(lines);
        }
    }

    /**
     * @brief Helper to create an InputStream from provided lines.
     * @param lines each element becomes a line of input (newline appended)
     * @return InputStream providing those lines to Scanner
     */
    static InputStream linesToInputStream(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @brief Simulates an interactive session and verifies printed output.
     *
     * Simulated input:
     * - "exit"       -> ends session
     *
     * This test injects a stub InputStream and RecordingPrintStream into the
     * genStack overload and inspects the recorded output.
     *
     * @throws Exception on unexpected errors from the tested method
     */
    /*@Test
    void testGenStack_withStubs() throws Exception {
        // Arrange
        InputStream testIn = linesToInputStream("exit");
        RecordingPrintStream testOut = new RecordingPrintStream();

        // Act
        GenStack.genStack(testIn, testOut, new String[0]);

        // Assert - check important prompts and results were printed
        List<String> out = testOut.getLines();
        assertTrue(out.stream().anyMatch(s -> s.contains("Thank you for using. Goodbye!")),
                   () -> "Goodbye message missing. Output:\n" + String.join("\n", out));
    }*/


    /**
     * @brief Simulates an interactive session and verifies printed output.
     *
     * Simulated input:
     * - "exit"       -> ends session
     *
     * The test temporarily replaces System.in and System.out, runs
     * genStack, then restores the original streams.
     *
     * @throws Exception if the tested method throws an unexpected exception
     */
    /*@Test
    public void testGenTask() throws Exception {
        String userInput = "exit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(in);

        // Capture System.out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // Run the CLI method under test
            GenStack.genStack(System.in, System.out,new String[0]);

            // Read captured output
            String output = outContent.toString();

            // Basic assertions: prompts and results present
            assertTrue(output.contains("Thank you for using. Goodbye!"));
        } finally {
            // Restore original streams
            System.setOut(originalOut);
            System.setIn(System.in);
        }
    }*/
}
