package org.openmicroscopy.shoola.env.config;

// Java imports 
import org.w3c.dom.Node;

/**
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 *              <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 *              <a href="mailto:a.falconi@dundee.ac.uk">a.falconi@dundee.ac.uk</a>
 */

class IntegerEntry extends Entry {
    
    private Integer value;
    IntegerEntry() {
    }
    protected void setContent(Node node) { 
        try {
            Node child = node.getFirstChild(); // has only one child
            value = new Integer(child.getNodeValue());
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    Object getValue() {
        return value; 
    }
}
