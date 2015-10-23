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
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.DirectoryScanner;
import org.python.core.PyList;
import org.python.util.PythonInterpreter;

/**
 * Generate a C++ test runner using the CxxTest test framework.
 */
@Mojo( name = CxxTestGenMojo.MOJO_NAME, defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES )
public class CxxTestGenMojo extends AbstractMSBuildPluginMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "cxxtestgen";
    
    public static File getCxxTestPythonHome( File cxxTestHome ) 
    {
        return new File( cxxTestHome, "python" );
    }
    
    
    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        if ( !isCxxTestEnabled( "runner generation", false ) ) 
        {
            return;
        }
        
        validateCxxTestConfiguration();
        initPythonIntepreter();

        for ( String testTarget : cxxTest.getTestTargets() ) 
        {
            final File targetPath = new File( projectFile.getParentFile(), testTarget );
            File testRunnerFile = new File( targetPath, cxxTest.getTestRunnerName() );
            if ( upToDate( targetPath, testRunnerFile ) )
            {
                getLog().info( "Skipping test generate as " + testTarget + " is up to date." );
            }
            else
            {
                try 
                {
                    List<String> arguments = getCxxTestGenArguments( testTarget, targetPath, testRunnerFile );
                    runCxxTestGen( testTarget, arguments );
                }
                catch ( MojoExecutionException mee )
                {
                    getLog().error( mee.getMessage() );
                    throw mee;
                }
            }
        }
    }

    private boolean upToDate( File targetPath, File testRunnerFile )
    {
        if ( ! testRunnerFile.exists() )
        {
            return false;
        }

        final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes( new String[]{ cxxTest.getTestHeaderPattern() } );
        directoryScanner.setBasedir( targetPath );
        directoryScanner.scan();

        getLog().debug( "Test runner last modified " + dateFormat.format( testRunnerFile.lastModified() ) );
        for ( String fileName : directoryScanner.getIncludedFiles() )
        {
            final File file = new File( targetPath, fileName );
            getLog().debug( fileName + " last modified " + dateFormat.format( file.lastModified() ) );
            if ( FileUtils.isFileNewer( file, testRunnerFile ) )
            {
                return false;
            }
        }
        return true;
    }

    private List<String> getCxxTestGenArguments( String testTarget, File targetPath, File outputFile )
            throws MojoExecutionException
    {
        List<String> arguments = new LinkedList<String>();
        String testTargetName = new File ( testTarget ).getName();
        
        arguments.add( "--have-eh" );
        arguments.add( "--abort-on-fail" );
        arguments.add( "--output=" + outputFile.getAbsolutePath() );
        arguments.add( "--xunit-printer" );
        
        File templateFile = getTemplateFileForTarget( targetPath );
        if ( templateFile != null )
        {
            arguments.add( "--template=" + templateFile.getAbsolutePath() );
        }
        else
        {
            
            arguments.add( "--xunit-file=" + cxxTest.getReportName() + "-" + testTargetName + ".xml" );
            getLog().debug( "Template file not set, reverting to internal template" );
        }
        
        arguments.add( new File( targetPath, cxxTest.getTestHeaderPattern() ).getAbsolutePath() );
                
        return arguments;
    }
    
    private File getTemplateFileForTarget( File targetPath ) throws MojoExecutionException
    {
        File templateFile = cxxTest.getTemplateFile();
        if ( cxxTest.getTemplateFile() != null )
        {
            if ( templateFile.exists() )
            {                
                return templateFile;
            }
            else if ( new File( targetPath, templateFile.getName() ).exists() )
            {
                return new File( targetPath, templateFile.getName() );
            }
            else
            {
                throw new MojoExecutionException( "Template file " + templateFile + " not found", 
                        new FileNotFoundException( templateFile.getAbsolutePath() ) );
            }
            
        }        
        return null;
    }
    
    private void runCxxTestGen( String testTarget, List<String> arguments ) throws MojoExecutionException
    {
        final String cxxTestGenArgVar = "cxxTestGenArgs";
        
        String cxxTestGenPath = cxxTest.getCxxTestHome().getAbsolutePath();
        PyList cxxTestGenArgs = new PyList( arguments );
        cxxTestGenArgs.add( 0, cxxTestGenPath );
        
        getLog().info( "Executing test runner generation for target " + testTarget + "." );
        getLog().debug( "Executing Python script " + cxxTestGenPath + " with arguments=" + cxxTestGenArgs );

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.exec( "import cxxtest" );
        resetCxxTestSuites( pythonInterpreter );
        
        pythonInterpreter.set( cxxTestGenArgVar, cxxTestGenArgs );
        pythonInterpreter.exec( "cxxtest.main(" + cxxTestGenArgVar + ")" );  
        pythonInterpreter.cleanup();
        
        getLog().info( "Test runner generation for target " + testTarget + " succeeded." );
    }
    
    private void initPythonIntepreter()
    {
        Properties postProperties = new Properties();
        
        postProperties.put( "python.path", getCxxTestPythonHome( cxxTest.getCxxTestHome() ).getAbsolutePath() );
        getLog().debug( "Initialising Jython with properties=" + postProperties );
        PythonInterpreter.initialize( System.getProperties(), postProperties, null );
    }
    
    /**
     * Releases of CxxTest previous to 4.3 do not reset the test parser state between consecutive runs (this seems to be
     * a bug). As a consequence, after CxxTest generates the test runner for the first test target, the tests for the  
     * first target are also included into the other test targets (if there are any), but that should not happen!
     * In order to prevent this issue, we manually reset the parser state using the Python statements below. 
     * This has been solved in version 4.3 (see https://github.com/CxxTest/cxxtest/commit/
     * 25cd65fa6db552955fcac4dbb5ed9d6b743dc613#diff-8e2015e71b643c6c0beaeca4ea81ab88)
     */
    private void resetCxxTestSuites( PythonInterpreter pythonInterpreter )
    {
        String cxxTestversion = pythonInterpreter.eval( "cxxtest.__release__.__version__" ).toString();
        
        if ( cxxTestversion.compareTo( "4.3" ) < 0 )
        {
            pythonInterpreter.exec( "cxxtest.cxxtest_parser.suite = None" );  
            pythonInterpreter.exec( "cxxtest.cxxtest_parser.suites = []" );  
            pythonInterpreter.exec( "cxxtest.cxxtest_parser.inBlock = 0" );  
        }
    }
}
