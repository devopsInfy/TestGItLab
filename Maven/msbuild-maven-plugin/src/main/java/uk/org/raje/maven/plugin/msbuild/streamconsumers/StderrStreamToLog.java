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

package uk.org.raje.maven.plugin.msbuild.streamconsumers;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * StreamConsumer that writes lines from a stream to the supplied Log at 'error' level.
 */
public class StderrStreamToLog implements StreamConsumer
{
    /**
     * Construct an instance to output standard error to the specified Log
     * @param logger the Log to write to
     */
    public StderrStreamToLog( Log logger ) 
    {
        this.logger = logger;
    }
    
    @Override
    public void consumeLine( String line )
    {
        logger.error( line );
    }
    
    private Log logger;
}
