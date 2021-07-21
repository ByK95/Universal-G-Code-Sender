package com.willwinder.universalgcodesender.utils;

import com.willwinder.universalgcodesender.types.GcodeCommand;

import java.io.*;
import java.util.HashMap;
import java.util.TreeSet;

public class GcodeCachedStreamReader extends GcodeStream implements IGcodeStreamReader {
    private HashMap<Integer, String> lines;
    private TreeSet<Integer> operations;
    private TreeSet<Integer> movements;
    private BufferedReader reader;
    private int numRows;
    private int line_index;
    private int numRowsRemaining;

    public GcodeCachedStreamReader(BufferedReader reader) throws GcodeStreamReader.NotGcodeStreamFile {
        this.reader = reader;
        line_index = 0;
        lines = new HashMap<>();
        operations = new TreeSet<>();
        movements = new TreeSet<>();
        try {
            String metadata = reader.readLine().trim();

            if (!metadata.startsWith(super.metaPrefix)) {
                throw new GcodeStreamReader.NotGcodeStreamFile();
            }

            metadata = metadata.substring(super.metaPrefix.length());
            numRows = Integer.parseInt(metadata);
            numRowsRemaining = numRows;
            for (int i = 0; i < numRows + 1; i++){
                String row = reader.readLine();
                if(row != null) {
                    lines.put(i, row);
                    if(row.contains("movement")){
                        movements.add(i);
                    }
                    if(row.contains("operation")){
                        operations.add(i);
                    }
                }
            }
            reader.close();
        } catch (Exception e ) {
            throw new GcodeStreamReader.NotGcodeStreamFile();
        }
    }

    public GcodeCachedStreamReader(File f) throws GcodeStreamReader.NotGcodeStreamFile, FileNotFoundException {
        this(new BufferedReader(new FileReader(f)));
    }

    @Override
    public boolean ready() {
        return numRowsRemaining > 0;
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumRowsRemaining() {
        return numRowsRemaining;
    }

    private String[] parseLine(String line) {
        return splitPattern.split(line, -1);
    }

    public int jumpToLine(int line_number){
        int skipped_cmd_count = line_number - line_index;
        line_index = line_number;
        numRowsRemaining = numRows - line_index;
        return skipped_cmd_count;
    }

    @Override
    public GcodeCommand getNextCommand() throws IOException {
        if (numRowsRemaining == 0) return null;


        String[] nextLine = parseLine(lines.get(line_index));
        if (nextLine.length != NUM_COLUMNS) {
            throw new IOException("Corrupt data found while processing gcode stream: " + lines.get(line_index));
        }
        numRowsRemaining--;
        line_index++;
        return new GcodeCommand(
                nextLine[COL_PROCESSED_COMMAND],
                nextLine[COL_ORIGINAL_COMMAND],
                nextLine[COL_COMMENT],
                Integer.parseInt(nextLine[COL_COMMAND_NUMBER]),
                false);
    }

    @Override
    public void close() throws IOException {
        return;
    }
}
