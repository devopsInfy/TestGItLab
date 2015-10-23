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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.SonarConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;

/**
 * Generates a Sonar configuration file for each platform/configuration pair. The configuration file tells Sonar
 * where to find the source code, the CxxTest reports and the CppCheck report so that it can populate the database. 
 */
@Mojo( name = SonarConfigGeneratorMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class SonarConfigGeneratorMojo extends AbstractMSBuildPluginMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "sonar";
    
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        if ( ! isSonarEnabled( false ) )
        {
            return;
        }
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                generateSonarConfiguration( platform, configuration );
            }
        }
        
    }
    
    /**
     * Determine whether Sonar configuration generation is enabled by the configuration
     * @param quiet set to true to suppress logging
     * @return true if sonar configuration generation is enabled, false otherwise
     * @throws MojoExecutionException if the configured project or solution file is invalid or cannot be parsed
     */
    protected boolean isSonarEnabled( boolean quiet ) throws MojoExecutionException
    {
        if ( sonar.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( SonarConfiguration.SONAR_SKIP_MESSAGE 
                        + ", 'skip' set to true in the " + SonarConfiguration.SONAR_NAME + " configuration." );
            }
            
            return false;
        }
        
        validateProjectFile();
        platforms = MojoHelper.validatePlatforms( platforms );
        
        return true;
    }

    private PrintWriter createSonarConfigWriter( File configFile ) throws MojoExecutionException
    {
        try 
        {
            FileUtils.forceMkdir( configFile.getParentFile() );
            return new PrintWriter( new FileWriter( configFile ) );
        }
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Could not create " + SonarConfiguration.SONAR_NAME 
                    + " configuration file " + configFile, ioe );
        }
    }
        
    private void finaliseConfigWriter( Writer configWriter, File configFile ) throws MojoExecutionException
    {
        try 
        {
            configWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + SonarConfiguration.SONAR_NAME + " configuration " 
                    + configFile, ioe );
        }
    }
    
    private void generateSonarConfiguration( BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException
    {
        String platformConfigPattern = "-*-" + platform.getName() + "-" + configuration.getName();
        File configFile = getSonarConfigFile( platform, configuration );
        PrintWriter configWriter = createSonarConfigWriter( configFile );

        List<VCProject> vcProjects = getParsedProjects( platform, configuration, sonar.getExcludeProjectRegex() );
        List<String> vcProjectNames = new ArrayList<String>( vcProjects.size() );
        List<File> systemIncludeDirs = getSystemIncludeDirs();

        // The relative path from basedir to the target directory
        File targetRelPath;
        
        try
        {
            targetRelPath = 
                    getRelativeFile( mavenProject.getBasedir(), new File( mavenProject.getBuild().getDirectory() ) );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Failed to compute relative path for build directory", ioe ); 
        }
        
        for ( VCProject vcProject: vcProjects )
        {
            vcProjectNames.add( vcProject.getName() );
        }
        
        try 
        {
            generateMavenProjectInformation( configWriter );
            
            configWriter.println( "sonar.sources=." );
            configWriter.println( "sonar.language=c++" );
            configWriter.println( "sonar.modules=" + joinList( vcProjectNames ) );

            if ( isCppCheckEnabled( true ) )
            {
                // Note: Due to issues in the Sonar C++ Community Plugin the report files must
                // be under the module 'projectBaseDir'.
                configWriter.println( new StringBuilder().append( "sonar.cxx.cppcheck.reportPath=" )
                        .append( CppCheckMojo.REPORT_DIRECTORY ) 
                        .append( File.separator )
                        .append( cppCheck.getReportName() )
                        .append( platformConfigPattern )
                        .append( ".xml" ).toString().replace( "\\", "\\\\" ) );
            }       
            
            if ( isVeraEnabled( true ) )
            {
                configWriter.println( new StringBuilder().append( "sonar.cxx.vera.reportPath=" )
                        .append( VeraMojo.REPORT_DIRECTORY ) 
                        .append( File.separator )
                        .append( vera.getReportName() )
                        .append( platformConfigPattern )
                        .append( ".xml" ).toString().replace( "\\", "\\\\" ) );
            }             

            if ( isCxxTestEnabled( null, true ) )
            {
                configWriter.println( new StringBuilder().append( "sonar.cxx.xunit.reportPath=" )
                        .append( targetRelPath )
                        .append( File.separator )
                        .append( CxxTestRunnerMojo.REPORT_DIRECTORY ) 
                        .append( File.separator )
                        .append( cxxTest.getReportName() )
                        .append( platformConfigPattern )
                        .append( ".xml" ).toString().replace( "\\", "\\\\" ) );
            }

            for ( VCProject vcProject: vcProjects )
            {
                generateProjectSonarConfiguration( vcProject, systemIncludeDirs, configWriter ); 
            }
        } 
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Could not write " + SonarConfiguration.SONAR_NAME 
                    + " configuration file " + configFile, ioe );
        }
        
        finaliseConfigWriter( configWriter, configFile );
        getLog().info( "Written sonar configuration file " + configFile.getAbsolutePath() );
    }

    private void generateMavenProjectInformation( PrintWriter writer )
    {
        writer.println( "sonar.projectKey=" + mavenProject.getModel().getGroupId() + ":" 
                + mavenProject.getModel().getArtifactId() );

        writer.println( "sonar.projectName=" + mavenProject.getModel().getName() );
        writer.println( "sonar.projectVersion=" + mavenProject.getModel().getVersion() );

        if ( mavenProject.getUrl() != null )
        {
            writer.println( "sonar.links.homepage=" + mavenProject.getUrl() );
        }
        
        if ( mavenProject.getCiManagement() != null
                && mavenProject.getCiManagement().getUrl() != null )
        {
            writer.println( "sonar.links.ci=" + mavenProject.getCiManagement().getUrl() );
        }
        
        if ( mavenProject.getIssueManagement() != null
                && mavenProject.getIssueManagement().getUrl() != null )
        {
            writer.println( "sonar.links.issue=" +  mavenProject.getIssueManagement().getUrl() );
        }
        
        if ( mavenProject.getScm() != null )
        {
            if ( mavenProject.getScm().getUrl() != null )
            {
                writer.println( "sonar.links.scm=" + mavenProject.getScm().getUrl() );
            }
            
            if ( mavenProject.getScm().getDeveloperConnection() != null )
            {
                writer.println( "sonar.links.scm_dev=" + mavenProject.getScm().getDeveloperConnection() );
            }
        }
    }

    private void generateProjectSonarConfiguration( VCProject vcProject, List<File> systemIncludeDirs, 
            PrintWriter writer ) throws IOException, MojoExecutionException
    {
        List<File> includeDirectories = new ArrayList<File>( vcProject.getIncludeDirectories() );
        includeDirectories.addAll( systemIncludeDirs );
        
        List<String> preprocessorDefs = new ArrayList<String>( vcProject.getPreprocessorDefs() );
        preprocessorDefs.addAll( sonar.getPreprocessorDefs() );
        
        writer.println( vcProject.getName() + ".sonar.projectBaseDir=" 
                + getRelativeFile( vcProject.getBaseDirectory(),
                        vcProject.getFile().getParentFile() ).getPath().replace( "\\", "\\\\" ) );
        
        if ( includeDirectories.size() > 0 )
        {
            List<String> includeDirectoryStr = new ArrayList<String>( includeDirectories.size() );
            
            for ( File includeDirectory : includeDirectories )
            {
                includeDirectoryStr.add( includeDirectory.getPath() );
            }
            
            writer.println( vcProject.getName() 
                    + ".sonar.cxx.include_directories=" 
                    + joinList( includeDirectoryStr ).replace( "\\", "\\\\" ) );
        }
            
        if ( preprocessorDefs.size() > 0 )
        {
            writer.println( vcProject.getName() + ".sonar.cxx.defines=" + joinList( preprocessorDefs ) );
        }
        
        if ( sonar.getExcludes().size() > 0 )
        {
            writer.println( vcProject.getName() + ".sonar.exclusions=" + joinList( sonar.getExcludes() ) );
        }
    }
    
    private List<File> getSystemIncludeDirs()
    {
        List<File> systemIncludeDirs = new ArrayList<File>();
        
        String systemIncludeDirsStr = msbuildSystemIncludes;
        if ( systemIncludeDirsStr == null )
        {
            systemIncludeDirsStr = System.getenv( "INCLUDE" );
        }
        
        if ( systemIncludeDirsStr != null )
        {
            for ( String includeDir : systemIncludeDirsStr.split( ";" ) )
            {
                systemIncludeDirs.add( new File( includeDir ) );
            }
        }
        
        return systemIncludeDirs;
    }
            
    private File getSonarConfigFile( BuildPlatform platform, BuildConfiguration configuration )
    {
        return new File( mavenProject.getBuild().getDirectory(), "sonar-configuration-" 
                + platform.getName() + "-" + configuration.getName() + ".properties" );
    }
    
    private String joinList( List<String> list )
    {
        StringBuilder builder = new StringBuilder();
        
        for ( String item : list )
        {
            builder.append( builder.length() > 0 ? "," + item : item );
        }
        
        return builder.toString();
    }

}
