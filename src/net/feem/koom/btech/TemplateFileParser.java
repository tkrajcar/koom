/*
 * Copyright 2009 Tim Krajcar <allegro@conmolto.org>.
 *
 * This file is part of Koom, a BattleTech MUX graphical HUD client.
 *
 * Koom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Koom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Koom.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.feem.koom.btech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.feem.koom.services.Resources;

/**
 * Parser for BTMUX mech/vehicle template files. The only specification for this
 * seems to be BTMUX's template.c, so all of this is a bit of guess-work.
 * 
 * <p>
 * Template files are a line-oriented format, with each line composing a
 * separate "command," and whitespace generally ignored.
 * </p>
 * 
 * @author cu5
 */
public class TemplateFileParser {
    // Parses lines of the form:
    // COMMAND { PARAMETERS }
    // while removing leading and trailing whitespace.
    private static final Pattern linePat = Pattern.compile("\\s*"
            + "((?:\\s*[^\\s{]+)*)" + "\\s*"
            + "(?:\\{\\s*((?:\\s*[^\\s}]+)*)\\s*\\})?" + "\\s*");

    // Parses strings of the form: CRIT_<number>[-<number>]
    private static final Pattern critPat = Pattern
            .compile("CRIT_([0-9]+)(?:-([0-9]+))?");

    private static final Map<String, CommandHandler> commands = new HashMap<String, CommandHandler>();

    private final Matcher lineMat = linePat.matcher("");
    private final Matcher critMat = critPat.matcher("");

    /*
     * Parse state.
     */
    private int lineCount;

    private String name;
    private String refName;
    private TemplateType type;
    private TemplateMoveType moveType;
    private final Map<String, String> misc = new HashMap<String, String>();

    private Section currentSection;
    private final Map<String, Section> sections = new HashMap<String, Section>();

    private static String canonicalize(String command) {
        return command.toUpperCase(Locale.ENGLISH);
    }

    public Template parse(InputStream input) throws IOException {
        // Set up a reader for the template file.
        Reader inputReader = Resources.getInputStreamReader(input);
        BufferedReader reader = new BufferedReader(inputReader);

        // Parse lines.
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                // Extract command and parameters.
                lineCount++;

                lineMat.reset(line);
                if (!lineMat.matches()) {
                    throw createParseError();
                }

                String command = lineMat.group(1);
                String parameters = lineMat.group(2);

                if (command.isEmpty()) {
                    if (parameters == null) {
                        // Completely blank line.
                        continue;
                    } else {
                        throw createParseError();
                    }
                }

                // Dispatch command.
                command = canonicalize(command);
                if (parameters == null) {
                    // Section header.
                    currentSection = new Section();
                    sections.put(command, currentSection);
                    continue;
                }

                CommandHandler handler = commands.get(command);
                if (handler == null) {
                    // No handler.
                    if (currentSection == null) {
                        misc.put(command, parameters);
                    } else {
                        currentSection.misc.put(command, parameters);
                    }
                } else {
                    // Delegate to handler.
                    if (currentSection == null) {
                        handler.run(this, parameters);
                    } else {
                        handler.run(this, parameters, currentSection);
                    }
                }
            }

            // Try to instantiate template.
            return createTemplate();
        } finally {
            resetState();
        }
    }

    private void resetState() {
        lineCount = 0;

        name = null;
        refName = null;
        type = null;
        moveType = null;
        misc.clear();

        currentSection = null;
        sections.clear();
    }

    private Template createTemplate() throws IOException {
        // Construct the correct type of template.
        if (type == null) {
            throw new IOException("Template has no type");
        }

        if (moveType == null) {
            throw new IOException("Template has no move type");
        }

        BasicTemplate template = new BasicTemplate(type, moveType);

        // Assign template properties.
        template.setName(name);
        template.setRef(refName);

        for (Map.Entry<String, String> miscEntry : misc.entrySet()) {
            // Unrecognized property.
            template.setProperty(miscEntry.getKey(), miscEntry.getValue());
        }

        // Assign section properties.
        for (Map.Entry<String, Section> entry : sections.entrySet()) {
            Section value = entry.getValue();
            TemplateSection section = template.addSection(entry.getKey());

            section.setArmor(value.armor);
            section.setInternals(value.internals);
            section.setRear(value.rear);

            for (Map.Entry<String, String> miscEntry : value.misc.entrySet()) {
                String miscName = miscEntry.getKey();

                critMat.reset(miscName);
                if (critMat.matches()) {
                    // CRIT_#[-#]
                    int idx1 = Integer.parseInt(critMat.group(1));

                    String prt2 = critMat.group(2);
                    int idx2 = (prt2 == null) ? idx1 : Integer.parseInt(prt2);

                    for (int ii = idx1; ii <= idx2; ii++) {
                        section.setCrit(ii, miscEntry.getValue());
                    }
                } else {
                    // Unrecognized property.
                    section.setProperty(miscName, miscEntry.getValue());
                }
            }
        }

        return template;
    }

    private IOException createParseError() {
        return new IOException("Parse error: line " + lineCount);
    }

    private IOException createParseError(String msg) {
        return new IOException("Parse error: line " + lineCount + ": " + msg);
    }

    /*
     * Command handlers.
     */
    private static class CommandHandler {
        void run(TemplateFileParser parser, String params, Section section)
                throws IOException {
            throw parser.createParseError("Not supported in section");
        }

        void run(TemplateFileParser parser, String params) throws IOException {
            throw parser.createParseError("Not supported outside of section");
        }
    }

    private static final class Section {
        private int armor = -1;
        private int internals = -1;
        private int rear = -1;

        private final Map<String, String> misc = new HashMap<String, String>();
    }

    static {
        // Friendly name.
        commands.put("NAME", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params) {
                parser.name = params;
            }
        });

        // Reference.
        commands.put("REFERENCE", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params) {
                parser.refName = params;
            }
        });

        // Type.
        commands.put("TYPE", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params) {
                parser.type = TemplateType.parse(canonicalize(params));
            }
        });

        // Move type.
        commands.put("MOVE_TYPE", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params) {
                parser.moveType = TemplateMoveType.parse(canonicalize(params));
            }
        });

        // (Front) armor hitpoints.
        commands.put("ARMOR", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params,
                    Section section) {
                section.armor = Integer.parseInt(params);
            }
        });

        // Internal hitpoints.
        commands.put("INTERNALS", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params,
                    Section section) {
                section.internals = Integer.parseInt(params);
            }
        });

        // Rear armor hitpoints.
        commands.put("REAR", new CommandHandler() {
            @Override
            public void run(TemplateFileParser parser, String params,
                    Section section) {
                section.rear = Integer.parseInt(params);
            }
        });
    }

    // For debugging purposes only.
    public static void main(String[] args) throws IOException {
        final TemplateStore store = new TemplateStore();

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                System.in, "UTF-8"));

        boolean first = true;

        String filename;
        while ((filename = inputReader.readLine()) != null) {
            if (first) {
                first = false;
            } else {
                System.out.println();
            }

            Template template = store.getTemplate(filename);

            System.out.format("%s (%s)%n", template.getName(), template
                    .getRef());
            System.out.format("Type: %s (%s)%n", template.getType(), template
                    .getMoveType());

            for (String sectionName : template.getSectionNames()) {
                TemplateSection section = template.getSection(sectionName);

                System.out.format("Section [%d/%d/%d]: %s%n", section
                        .getArmor(), section.getInternals(), section.getRear(),
                        sectionName);
                for (int ii = 1; ii <= section.getCritCount(); ii++) {
                    String crit = section.getCrit(ii);
                    System.out.format("\tcrit #%2d: %s%n", ii, crit);
                }
            }
        }
    }
}
