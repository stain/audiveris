//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m H e a d R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

/**
 * Class {@code BeamHeadRelation}
 *
 * @author Hervé Bitteur
 */
public class BeamHeadRelation
        extends BasicSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamHeadRelation} object.
     *
     * @param grade quality of relation
     */
    public BeamHeadRelation (double grade)
    {
        super(grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getName ()
    {
        return "Beam-Head";
    }

    @Override
    protected double getSourceCoeff ()
    {
        return constants.beamSupportCoeff.getValue();
    }

    @Override
    protected double getTargetCoeff ()
    {
        return constants.headSupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio beamSupportCoeff = new Constant.Ratio(0, "Supporting coeff for beam");

        final Constant.Ratio headSupportCoeff = new Constant.Ratio(
                0.75,
                "Supporting coeff for head");
    }
}