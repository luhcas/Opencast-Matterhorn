/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *  
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** @author Christoph E. Driessen <ced@neopoly.de> */
public abstract class ProcessExecutor {

    private boolean redirectErrorStream = true;
    private String[] commandLine;

    protected ProcessExecutor(String commandLine) {
        this.commandLine = commandLine.split("\\s+");
    }

    protected ProcessExecutor(String command, String options) {
        commandLine = new ArrayBuilder<String>(String.class)
                .add(command).add(options.split("\\s+")).toArray();
    }

    protected ProcessExecutor(String[] commandLine) {
        this.commandLine = commandLine;
    }

    protected ProcessExecutor(String commandLine, boolean redirectErrorStream) {
        this(commandLine);
        this.redirectErrorStream = redirectErrorStream;
    }

    protected ProcessExecutor(String[] commandLine, boolean redirectErrorStream) {
        this(commandLine);
        this.redirectErrorStream = redirectErrorStream;
    }

    protected ProcessExecutor(String command, String options, boolean redirectErrorStream) {
        this(command, options);
        this.redirectErrorStream = redirectErrorStream;
    }

    public void execute() {
        BufferedReader in = null;
        Process process = null;
        try {
            // create process.
            // no special working dir is set which means the working dir of the
            // current java process is used.
            ProcessBuilder pbuilder = new ProcessBuilder(commandLine);
            pbuilder.redirectErrorStream(redirectErrorStream);
            process = pbuilder.start();
            // Consume error stream if necessary
            if (!redirectErrorStream) {
                new StreamHelper(process.getErrorStream());
            }
            // Read input and
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (!onLineRead(line)) break;
            }

            // wait until the task is finished
            process.waitFor();
            int exitCode = process.exitValue();
            onProcessFinished(exitCode);
        } catch (Exception e) {
            onError(e);
        } finally {
        	IoSupport.closeQuietly(process);
            IoSupport.closeQuietly(in);
        }
    }

    protected boolean onLineRead(String line) {
        return false;
    }

    protected void onProcessFinished(int exitCode) {
    }

    protected void onError(Exception e) {
        throw new RuntimeException(e);
    }

}
