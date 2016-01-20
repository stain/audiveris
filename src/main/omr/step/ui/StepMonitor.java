//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t e p M o n i t o r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step.ui;

import omr.constant.Constant;
import omr.constant.Constant.Ratio;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Class {@code StepMonitor} is the user interface entity that allows to monitor step
 * progression.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class StepMonitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StepMonitor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Progress bar for actions performed on sheet. */
    private final JProgressBar bar = new MyJProgressBar();

    /** Total active actions. */
    private int actives = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a user monitor on step processing.
     * There is exactly one instance of this class (and no instance when running in batch mode)
     */
    public StepMonitor ()
    {
        bar.setToolTipText("On going Step activity");
        bar.setStringPainted(true);
        displayAnimation(false);
        bar.setString("");
        bar.setForeground(Color.ORANGE);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the monitoring bar
     *
     * @return the step progress bar
     */
    public JProgressBar getComponent ()
    {
        return bar;
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Display a simple message in the progress window.
     *
     * @param msg the message to display
     */
    public void notifyMsg (final String msg)
    {
        logger.debug("notifyMsg '{}'", msg);
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        bar.setString(msg);
                    }
                });
    }

    //------------------//
    // displayAnimation //
    //------------------//
    /**
     * Switch on or off the display of the progress bar
     *
     * @param animating If false, deactivates all animation of the progress bar.
     *                  If true, activates an indeterminate or pseudo-indeterminate animation.
     */
    final synchronized void displayAnimation (final boolean animating)
    {
        logger.debug("displayAnimation animating:{}", animating);

        if (animating) {
            actives++;
            setBar(constants.ratio.getValue());

            if (constants.useIndeterminate.isSet()) {
                bar.setIndeterminate(true);
            }
        } else {
            if (actives > 0) {
                actives--;
            }

            if (actives <= 0) {
                setBar(0);
                bar.setIndeterminate(false);
            }
        }
    }

    //---------//
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage a certain amount above the previous
     * percentage value (or above 0 if the bar had been indeterminate).
     * This method is called on every message logged (see LogStepMonitorHandler)
     */
    void animate ()
    {
        if (!constants.useIndeterminate.isSet()) {
            logger.debug("animate");
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        @Override
                        public void run ()
                        {
                            int old = bar.getValue();

                            if (old > bar.getMinimum()) {
                                int diff = bar.getMaximum() - old;
                                int increment = (int) Math.round(diff * constants.ratio.getValue());

                                bar.setValue(old + increment);
                            }
                        }
                    });
        }
    }

    //--------//
    // setBar //
    //--------//
    /**
     * Sets the progress bar to show a percentage.
     *
     * @param amount percentage, in decimal form, from 0.0 to 1.0
     */
    private void setBar (final double amount)
    {
        logger.debug("setBar amount:{}", amount);
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        int divisions = constants.divisions.getValue();
                        bar.setMinimum(0);
                        bar.setMaximum(divisions);

                        int val = (int) Math.round(divisions * amount);
                        bar.setValue(val);
                    }
                });
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer divisions = new Constant.Integer(
                "divisions",
                1000,
                "Number of divisions (amount of precision) of step monitor, minimum 10");

        private final Ratio ratio = new Ratio(
                0.1,
                "Amount by which to increase step monitor percentage per animation, between 0 and 1");

        private final Constant.Boolean useIndeterminate = new Constant.Boolean(
                true,
                "Should we use an indeterminate step progress bar?");
    }

    //----------------//
    // MyJProgressBar //
    //----------------//
    private static class MyJProgressBar
            extends JProgressBar
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void paintComponent (Graphics g)
        {
            try {
                super.paintComponent(g);
            } catch (Exception ex) {
                // Nearly ignored
                repaint(); // To trigger another painting
            }
        }
    }
}