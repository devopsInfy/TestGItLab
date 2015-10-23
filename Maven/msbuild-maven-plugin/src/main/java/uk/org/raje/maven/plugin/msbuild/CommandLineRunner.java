/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Masato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.raje.maven.plugin.msbuild;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;

/**
 * This class runs a native process on the command-line and catches the process' standard output and standard error 
 * streams. It offers support for:
 * <ul>
 *      <li>Setting the process path and arguments on the command-line.</li>
 *      <li>Setting the working directory from which the process will run.</li>
 *      <li>Injecting variables into the process environment.</li>
 * </ul>
 * Derived classes must override {@link CommandLineRunner#getCommandLineArguments()} to provide the process path and 
 * arguments for the command-line. 
 */
public abstract class CommandLineRunner 
{
    /**
     * Create a new command-line process runner.
     * @param processName the name of the process being run (for log messages)
     * @param outputConsumer a consumer for the process' standard output 
     * @param errorConsumer a consumer for the process' standard error 
     * @see uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog
     * @see uk.org.raje.maven.plugin.msbuild.streamconsumers.StderrStreamToLog
     */
    public CommandLineRunner( String processName, StreamConsumer outputConsumer, StreamConsumer errorConsumer )
    {
        this.processName = processName;
        this.outputConsumer = outputConsumer;
        this.errorConsumer = errorConsumer;
    }

    /**
     * Execute the configured program
     * @return the exit code from the program
     * @throws IOException if there is a problem with program execution
     * @throws InterruptedException if we are interrupted waiting for the process to exit or streams to complete
     */
    public int runCommandLine() throws IOException, InterruptedException
    {
        logRunnerConfiguration();
        
        ProcessBuilder processBuilder = new ProcessBuilder( getCommandLineArguments() );
        processBuilder.directory( workingDirectory );
        processBuilder.environment().putAll( environmentVars );
        Process commandLineProc = processBuilder.start();

        final StreamPumper stdoutPumper = new StreamPumper( commandLineProc.getInputStream(), outputConsumer );
        final StreamPumper stderrPumper = new StreamPumper( commandLineProc.getErrorStream(), errorConsumer );
        stdoutPumper.start();
        stderrPumper.start();
        
        if ( standardInputString != null )
        {
            OutputStream outputStream = commandLineProc.getOutputStream();
            outputStream.write( standardInputString.getBytes() );
            outputStream.close();
        }

        int exitCode = commandLineProc.waitFor();
        stdoutPumper.waitUntilDone();
        stderrPumper.waitUntilDone();
        
        if ( exitCode == 0 )
        {
            LOGGER.fine( processName + " returned zero exit code"  );
        }
        else 
        { 
            LOGGER.severe( processName + " returned non-zero exit code (" + exitCode + ")" );
        }
        
        return exitCode; 
    }    

    /**
     * Set the working directory for the process
     * @param workingDirectory the directory to set as the working directory
     */
    public void setWorkingDirectory( File workingDirectory ) 
    {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Specify a set of environment variables to setup before running the process 
     * @param environmentVars a Map containing environment name, value pairs
     */
    public void setEnvironmentVars( Map<String, String> environmentVars ) 
    {
        this.environmentVars = environmentVars;
    }

    /**
     * Specify an optional String that is sent to the processes stdin stream
     * @param standardInputString a String to write to stdin
     */
    public void setStandardInputString( String standardInputString ) 
    {
        this.standardInputString = standardInputString;
    }

    /**
     * Method that concrete implementations provide to construct the command line argument list for the process 
     * @return a List of Strings representing the command line arguments
     */
    protected abstract List<String> getCommandLineArguments();

    /**
     * Get the configured working directory
     * @return an abstract path for the working directory, "." if none was supplied
     */
    protected File getWorkingDirectory() 
    {
        return workingDirectory;
    }

    /**
     * Get the configured environment variables
     * @return a Map containing name, value pairs, by default an empty Map is created
     */
    protected Map<String, String> getEnvironmentVars() 
    {
        return environmentVars;
    }

    /**
     * Get the configured String supplied to stdin when the process is executed
     * @return the configured String or null if none was set
     */
    protected String getStandardInputString() 
    {
        return standardInputString;
    }

    private void logRunnerConfiguration()
    {
        StringBuilder commandLine = new StringBuilder();
        
        for ( String arg : getCommandLineArguments() )
        {
            commandLine.append( arg ).append( " " );
        }
                
        LOGGER.fine( processName + " command line:" );
        LOGGER.fine( "\t" + commandLine.toString() );
        LOGGER.fine( "Working directory:" );
        LOGGER.fine( "\t" + workingDirectory.getAbsolutePath() );
        
        if ( environmentVars.size() > 0 )
        {
            LOGGER.fine( "Environemnt variables:" );
            LOGGER.fine( "\t" + environmentVars.toString() );
        }

        if ( standardInputString != null )
        {
            LOGGER.fine( "Standard input:" );
            LOGGER.fine( "\t" + standardInputString );
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger( CommandLineRunner.class.getName() );
    
    private String processName;
    private StreamConsumer outputConsumer;
    private StreamConsumer errorConsumer;
    private File workingDirectory = new File( "." );
    private String standardInputString;
    private Map<String, String> environmentVars = new HashMap<String, String>();
}
