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
package net.feem.koom.net;

import java.io.IOException;

import net.feem.koom.services.Utility;

/**
 * A high level interface to the TELNET protocol. You should generally interface
 * with this class, in preference to using {@link TELNETProtocol} directly.
 * 
 * @author cu5
 */
public class ClientNVT extends AbstractNVT {
    private static final byte[] TERMINAL_TYPE = Utility.getASCII("KOOM");
    private static final int TERMINAL_TYPE_LEN = Math.min(40,
            TERMINAL_TYPE.length);

    private final TELNETEventHandler outputHandler;

    private final TTYPEOption optionTTYPE = new TTYPEOption();
    private final NAWSOption optionNAWS = new NAWSOption();

    public ClientNVT(TELNETProtocol proto) throws IOException {
        super(proto);
        proto.setInputHandler(new InputEventHandler());
        outputHandler = proto.getOutputEventHandler();
    }

    public void setWindowSize(int rows, int columns) throws IOException {
        optionNAWS.setWindowSize(rows, columns);
    }

    private class InputEventHandler extends AbstractNVT.InputEventHandler {
        @Override
        public void processDO(int option) throws IOException {
            switch (option) {
            case TTYPEOption.OPTION_CODE:
                optionTTYPE.enable();
                break;

            case NAWSOption.OPTION_CODE:
                optionNAWS.enable();
                break;

            default:
                super.processDO(option);
                break;
            }
        }

        @Override
        public void processDONT(int option) throws IOException {
            switch (option) {
            case TTYPEOption.OPTION_CODE:
                optionTTYPE.disable();
                break;

            case NAWSOption.OPTION_CODE:
                optionNAWS.disable();
                break;

            default:
                super.processDONT(option);
                break;
            }
        }

        @Override
        public void beginParam(int option) throws IOException {
            switch (option) {
            case TTYPEOption.OPTION_CODE:
                setSubOption(optionTTYPE);
                break;

            default:
                setSubOption(null);
                return;
            }

            super.beginParam(option);
        }
    }

    /**
     * Terminal Type option (RFC 1091).
     */
    private final class TTYPEOption extends TELNETOption {
        private static final int OPTION_CODE = 24;

        private static final byte SUB_IS = (byte) 0;
        private static final byte SUB_SEND = (byte) 1;

        private int paramCount;

        private TTYPEOption() {
            super(OPTION_CODE);
        }

        @Override
        public void beginParam() {
            paramCount = 1;
        }

        @Override
        public void endParam() throws IOException {
            if (paramCount == 2) {
                // Send TTYPE.
                paramCount = 0;

                synchronized (getWriteLock()) {
                    flushInput();
                    outputHandler.beginParam(OPTION_CODE);
                    outputHandler.appendParam(SUB_IS);

                    // Add TERMINAL_TYPE, truncated to 40 bytes.
                    for (int ii = 0; ii < TERMINAL_TYPE_LEN; ii++) {
                        outputHandler.appendParam(TERMINAL_TYPE[ii]);
                    }

                    outputHandler.endParam();
                    flushProtocol();
                }
            }
        }

        @Override
        public void appendParam(byte nextByte) {
            if (paramCount == 1 && nextByte == SUB_SEND) {
                // Request for TTYPE from server.
                paramCount = 2;
            } else {
                // Parameter parsing failed.
                paramCount = -1;
            }
        }

        @Override
        protected void requestEnable() throws IOException {
            synchronized (getWriteLock()) {
                flushInput();
                outputHandler.processWILL(OPTION_CODE);
                flushProtocol();
            }
        }

        @Override
        protected void requestDisable() throws IOException {
            synchronized (getWriteLock()) {
                flushInput();
                outputHandler.processWONT(OPTION_CODE);
                flushProtocol();
            }
        }
    }

    /**
     * Negotiate About Window Size option (RFC 1073).
     */
    private final class NAWSOption extends TELNETOption {
        private static final int OPTION_CODE = 31;

        // Unknown window size; call setWindowSize() to initialize.
        private int rows = 0;
        private int cols = 0;

        private NAWSOption() {
            super(OPTION_CODE);
        }

        public void setWindowSize(int rows, int cols) throws IOException {
            if (rows < 1 || rows > 65535 || cols < 1 || cols > 65535) {
                throw new IllegalArgumentException("Invalid window size");
            }

            if (rows == this.rows && cols == this.cols) {
                // No change.
                return;
            }

            if (isEnabled()) {
                sendWindowSize(rows, cols);
            }

            this.rows = rows;
            this.cols = cols;
        }

        @Override
        protected void requestEnable() throws IOException {
            synchronized (getWriteLock()) {
                flushInput();
                outputHandler.processWILL(OPTION_CODE);
                flushProtocol();
            }
        }

        @Override
        protected void requestDisable() throws IOException {
            synchronized (getWriteLock()) {
                flushInput();
                outputHandler.processWONT(OPTION_CODE);
                flushProtocol();
            }
        }

        @Override
        protected void doEnable() throws IOException {
            sendWindowSize(rows, cols);
        }

        private void sendWindowSize(int rows, int cols) throws IOException {
            if (rows == 0 && cols == 0) {
                // Unknown window size, don't send anything.
                return;
            }

            // Parameter: WIDTH[1] WIDTH[0] HEIGHT[1] HEIGHT[0]
            synchronized (getWriteLock()) {
                flushInput();
                outputHandler.beginParam(getOption());

                outputHandler.appendParam((byte) (cols >>> 8));
                outputHandler.appendParam((byte) cols);
                outputHandler.appendParam((byte) (rows >>> 8));
                outputHandler.appendParam((byte) rows);

                outputHandler.endParam();
                flushProtocol();
            }
        }
    }
}
