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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;

/**
 * Test CppCheckMojo configuration options.
 * Note: jUnit annotations are not observed as this class extends an old style test class.
 */
public class CppCheckMojoTest extends AbstractMSBuildMojoTestCase 
{
    public void setUp() throws Exception
    {
        super.setUp();
        
        outputStream = new ByteArrayOutputStream();
        System.setOut( new PrintStream( outputStream ) );
    }

    @Test
    public final void testAllSettingsConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( cppCheckMojo );
    }

    @Test
    public final void testMissingCppCheckConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/missing-cppcheck-path-pom.xml" ) ;

        assertNull( cppCheckMojo.cppCheck.getCppCheckPath() );

        cppCheckMojo.execute();
        
        assertEquals( "[INFO] Skipping static code analysis, path to CppCheck not set",
                outputStream.toString().trim() );
    }
    
    @Test
    public final void testMinimalSolutionConfiguration() throws Exception 
    {
        ( (DefaultPlexusContainer) getContainer() ).getLoggerManager().setThreshold( Logger.LEVEL_DEBUG );
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/sln-single-platform-single-config-pom.xml" );

        cppCheckMojo.execute();
    }    

    @Test
    public final void testSolutionExcludesConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/sln-single-platform-single-config-excludes-pom.xml" );

        cppCheckMojo.execute();
        
        assertFalse( "Unexpected error log", outputStream.toString().contains( "[ERROR]" ) );
        assertTrue( "Expected '[INFO] Static code analysis complete' not present in log",
                outputStream.toString().contains( "[INFO] Static code analysis complete" ) );
    }    

    private ByteArrayOutputStream outputStream;
}
