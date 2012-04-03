//----------------------------------------------------------------------------//
//                                                                            //
//                                P l u g i n                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.plugin;

import omr.log.Logger;

import omr.score.Score;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BasicTask;
import omr.util.FileUtil;

import org.jdesktop.application.Task;

import java.io.*;
import java.util.Collections;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Class {@code Plugin} describes a plugin instance, encapsulating the
 * relationship with the  underlying javascript file.
 *
 * <p>A plugin is meant to describe the connection between Audiveris and an
 * external program, which will consume the MusicXML file exported by Audiveris.</p>
 *
 * <p>A plugin is a javascript file, meant to export:
 * <dl>
 * <dt>pluginTitle</dt>
 * <dd>(string) The title to appear in Plugins pull-down menu</dd>
 * <dt>pluginTip</dt>
 * <dd>(string) A description text to appear as a user tip in Plugins menu</dd>
 * <dt>pluginCli</dt>
 * <dd>(function) A javascript function which returns the precise list of
 * arguments used when calling the external program. Note that the actual call
 * is not made by the javascript code, but by Audiveris itself for an easier
 * handling of input and output streams.</dd>
 * </dl>
 * @author Hervé Bitteur
 */
public class Plugin
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Plugin.class);

    //~ Instance fields --------------------------------------------------------

    /** Related javascript file */
    private final File file;

    /** Related engine */
    private ScriptEngine engine;

    /** Plugin title */
    private String title;

    /** Description used for tool tip */
    private String tip;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Plugin //
    //--------//
    /**
     * Creates a new Plugin object.
     * @param file related javascript file
     */
    public Plugin (File file)
    {
        this.file = file;

        evaluateScript();

        if (logger.isFineEnabled()) {
            logger.info("Created " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a descriptive sentence for this plugin.
     * @return a sentence meant for tool tip
     */
    public String getDescription ()
    {
        if (tip != null) {
            return tip;
        } else {
            // Default value
            return getId();
        }
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report a unique ID for this plugin.
     * @return plugin unique ID
     */
    public String getId ()
    {
        return FileUtil.getNameSansExtension(file);
    }

    //---------//
    // getTask //
    //---------//
    /**
     * Report the asynchronous plugin task on provided score.
     * @param score the score to process through this plugin
     */
    public Task getTask (Score score)
    {
        return new PluginTask(score);
    }

    //----------//
    // getTitle //
    //----------//
    /**
     * Report a title meant for user interface.
     * @return a title for this plugin
     */
    public String getTitle ()
    {
        if (title != null) {
            return title;
        } else {
            return getId();
        }
    }

    //---------//
    // perform //
    //---------//
    /**
     * Perform this plugin on the provided score instance.
     * @param score the score to process through this plugin
     */
    public void perform (Score score)
    {
        getTask(score)
            .execute();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ")
          .append(getId());

        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // evaluateScript //
    //----------------//
    /**
     * Evaluate the plugin script to get precise information built.
     */
    private void evaluateScript ()
    {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("JavaScript");

        try {
            InputStream is = new FileInputStream(file);
            Reader      reader = new InputStreamReader(is);
            engine.eval(reader);

            // Retrieve information from script
            title = (String) engine.get("pluginTitle");
            tip = (String) engine.get("pluginTip");
        } catch (Exception ex) {
            logger.warning(this + " error", ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // PluginTask //
    //------------//
    /**
     * Handles the processing defined by the underlying javascript.
     * The lifecycle of this instance is limited to the duration of the task.
     */
    private class PluginTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final Score score;

        //~ Constructors -------------------------------------------------------

        public PluginTask (Score score)
        {
            this.score = score;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        @SuppressWarnings("unchecked")
        protected Void doInBackground ()
            throws InterruptedException
        {
            // Make sure we have the export file
            Step exportStep = Steps.valueOf(Steps.EXPORT);

            if (!score.getFirstPage()
                      .getSheet()
                      .isDone(exportStep)) {
                logger.info("Getting export from " + score + " ...");
                Stepping.processScore(Collections.singleton(exportStep), score);
            }

            final File exportFile = score.getExportFile();

            if (exportFile == null) {
                logger.warning("Could not get export file");

                return null;
            }

            // Retrieve proper sequence of command items
            List<String> args;

            try {
                if (logger.isFineEnabled()) {
                    logger.info(
                        Plugin.this + " doInBackground on " + exportFile);
                }

                Invocable inv = (Invocable) engine;
                Object    obj = inv.invokeFunction(
                    "pluginCli",
                    exportFile.getAbsolutePath());

                args = (List<String>) obj; // Unchecked by compiler

                if (logger.isFineEnabled()) {
                    logger.info(Plugin.this + " command args: " + args);
                }
            } catch (Exception ex) {
                logger.warning(Plugin.this + " error invoking javascript", ex);

                return null;
            }

            // Spawn the command
            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb = pb.redirectErrorStream(true);

                Process           process = pb.start();
                InputStream       is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader    br = new BufferedReader(isr);

                // Consume process output
                String line;

                while ((line = br.readLine()) != null) {
                    if (logger.isFineEnabled()) {
                        logger.fine(line);
                    }
                }

                // Wait to get exit value
                try {
                    int exitValue = process.waitFor();

                    if (exitValue != 0) {
                        logger.warning(
                            Plugin.this + " exited with value " + exitValue);
                    } else if (logger.isFineEnabled()) {
                        logger.info(
                            Plugin.this + " exit value is " + exitValue);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException ex) {
                logger.warning(Plugin.this + " error launching editor", ex);
            }

            return null;
        }
    }
}
