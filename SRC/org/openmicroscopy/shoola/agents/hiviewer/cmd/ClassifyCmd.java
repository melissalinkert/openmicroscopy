/*
 * org.openmicroscopy.shoola.agents.hiviewer.cmd.ClassifyCmd
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.hiviewer.cmd;


//Java imports
import javax.swing.JFrame;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.hiviewer.browser.ImageDisplay;
import org.openmicroscopy.shoola.agents.hiviewer.clsf.Classifier;
import org.openmicroscopy.shoola.agents.hiviewer.clsf.ClassifierFactory;
import org.openmicroscopy.shoola.agents.hiviewer.view.HiViewer;
import org.openmicroscopy.shoola.env.data.model.ImageSummary;

/** 
 * Command to classify/declassify a given Image.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class ClassifyCmd
    implements ActionCmd
{
    
    /**
     * Utility method to get an {@link ImageSummary} from the 
     * <code>model</code>.
     * 
     * @param model The Model from which to extract the Image.
     * @return The {@link ImageSummary} hierarchy object in the browser's
     *         current Image node or <code>null</code> if the browser's
     *         current display is not an Image node. 
     */
    private static ImageSummary getImage(HiViewer model)
    {
        ImageSummary img = null;
        if (model != null) {
            ImageDisplay selDispl = model.getBrowser().getSelectedDisplay();
            Object x = selDispl.getHierarchyObject();
            if (x instanceof ImageSummary) img = (ImageSummary) x;
        }
        return img;
    }
    
    
    /** 
     * The classification mode.
     * This is one of the constants defined by the {@link Classifier} interface
     * and tells whether we're classifying or declassifying.
     */
    private int                 mode;
    
    /** 
     * Represents the Image to classify/declassify.
     * If <code>null</code>, no acion is taken.
     */
    private ImageSummary        img;
    
    /** The window from which this command was invoked. */
    private JFrame              owner;
    
    
    /**
     * Creates a new command to classify/declassify the specified Image.
     * 
     * @param img   Represents the Image to classify/declassify.
     *              If <code>null</code>, no acion is taken.
     * @param mode  The classification mode.  This is one of the constants 
     *              defined by the {@link Classifier} interface and tells 
     *              whether we're classifying or declassifying.
     * @param owner The window from which this command was invoked.
     *              Mustn't be <code>null</code>.
     */
    public ClassifyCmd(ImageSummary img, int mode, JFrame owner)
    {
        if (owner == null)
            throw new NullPointerException("No owner.");
        this.img = img;
        this.mode = mode;
    }
     
    /**
     * Creates a new command to classify/declassify the Image in the browser's
     * currently selected node, if the node is an Image node.
     * If the node is not an Image node, no action is taken.
     * 
     * @param model The Model which has a reference to the browser.
     *              Mustn't be <code>null</code>.
     * @param mode  The classification mode.  This is one of the constants 
     *              defined by the {@link Classifier} interface and tells 
     *              whether we're classifying or declassifying.
     */
    public ClassifyCmd(HiViewer model, int mode) 
    { 
        this(getImage(model), mode, model.getUI()); 
    }
    
    /** 
     * Classifies or declassifies the Image given to this command.
     * 
     * @see ActionCmd#execute() 
     */
    public void execute()
    {
        if (img == null) return;
        Classifier classifier = ClassifierFactory.createComponent(
                                                    mode, img.getID(),
                                                    owner);
        classifier.activate();
    }

}
