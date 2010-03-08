/*
 * org.openmicroscopy.shoola.env.data.login.UserCredentials
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.data.login;


//Java imports

//Third-party libraries

//Application-internal dependencies

/** 
 * Holds the user's credentials for logging onto <i>OMERO</i>.
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
public class UserCredentials
{
    
	/** Identifies a high speed connection. */
	public static final int HIGH = 0;
	
	/** Identifies a medium speed connection. */
	public static final int MEDIUM = 1;
	
	/** Identifies a low speed connection. */
	public static final int LOW = 2;
	
    /**
     * The <i>OMERO</i> login name of the user.
     * This is the <code>OME Name</code> that was assinged to the user when
     * it was created in the DB.
     */
    private String  userName;
    
    /**
     * The <i>OMERO</i> login password of the user.
     * This is the password that was chosen for the user when it was created
     * in the DB.
     */
    private String  password;
    
    /** The name of the <i>OMERO</i> server. */
    private String  hostName;
    
    /** The connection speed level. */
    private int 	speedLevel;
    
    /** The value of the port. */
    private int 	port;
    
    /** The selected group or <code>-1</code>. */
    private long 	group;
    
	/** Indicates that the experimenter to handle is an administrator. */
	private Boolean administrator;
	
	/** Indicates if the experimenter is the owner of the group. */
	private Boolean owner;
	
	/** Indicates to active or not the user. */ 
	private Boolean active;
	
    /** 
     * Controls if the passed speed index is supported.
     * 
     * @param level The value to handle.
     */
    private void checkSpeedLevel(int level)
    {
    	switch (level) {
			case HIGH:
			case MEDIUM:
			case LOW:
				return;
	
			default:
				throw new IllegalArgumentException("Speed level not valid.");
		}
    }
    
    /**
     * Creates a new instance.
     * 
     * @param userName The <i>OMERO</i> login name of the user.
     *                 This is the <code>OME Name</code> that was assigned to 
     *                 the user when it was created in the DB.
     * @param password The <i>OMERO</i> login password of the user.
     *                 This is the password that was chosen for the user when
     *                 it was created in the DB.
     * @param hostName 	 The name of the selected server.
     * @param speedLevel The connection speed.
     * @throws IllegalArgumentException If the user name and/or the password is
     *                 <code>null</code> or has <code>0</code>-length.
     */
    public UserCredentials(String userName, String password, String hostName,
    		int speedLevel)
    {
    	/*
        if (userName == null || userName.length() == 0)
            throw new IllegalArgumentException("Please specify a user name.");
        if (password == null || password.length() == 0)
            throw new IllegalArgumentException("Please specify a password.");
            */
    	checkSpeedLevel(speedLevel);
    	this.speedLevel = speedLevel;
        this.userName = userName;
        this.password = password;
        this.hostName = hostName;
        port = -1;
        group = -1L;
        owner = null;
        administrator = null;
    }
    
    /**
     * Creates a new instance.
     * 
     * @param userName The <i>OMERO</i> login name of the user.
     *                 This is the <code>OME Name</code> that was assigned to 
     *                 the user when it was created in the DB.
     * @param password The <i>OMERO</i> login password of the user.
     *                 This is the password that was chosen for the user when
     *                 it was created in the DB.
     * @throws IllegalArgumentException If the user name and/or the password is
     *                 <code>null</code> or has <code>0</code>-length.
     */
    public UserCredentials(String userName, String password)
    {
    	this(userName, password, "", HIGH);
    }
    
    /**
     * Sets the group.
     * 
     * @param group The value to set.
     */
    public void setGroup(long group) { this.group = group; }
    
    /**
     * Returns the group or <code>-1</code>.
     * 
     * @return See above.
     */
    public long getGroup() { return group; }
    
    /**
     * Sets the port.
     * 
     * @param port The value to set.
     */
    public void setPort(int port) { this.port = port; }
    
    /**
     * Resets the password.
     * 
     * @param password The <i>OMERO</i> login password of the user.
     */
    public void resetPassword(String password)
    {
    	 if (password == null || password.trim().length() == 0)
             throw new IllegalArgumentException("Please specify a password.");
    	 this.password = password;
    }
    
    /**
     * Returns the port used, if <code>-1</code>, the port is the value set
     * in the configuration file
     * 
     * @return See above.
     */
    public int getPort() { return port; }
    
    /**
     * Returns the name of the <i>OMERO</i> server.
     * 
     * @return See above.
     */
    public String getHostName() { return hostName; }
    
    /**
     * Returns the <i>OMERO</i> login name of the user.
     * This is the <code>OME Name</code> that was assinged to the user when
     * it was created in the DB.
     * This field is always a non-<code>null</code> string with a positive
     * length.
     * 
     * @return See above.
     */
    public String getUserName() { return userName; }

    /**
     * Returns the <i>OMERO</i> login password of the user.
     * This is the password that was chosen for the user when it was created
     * in the DB.
     * This field is always a non-<code>null</code> string with a positive
     * length.
     * 
     * @return See above.
     */
    public String getPassword() { return password; }
    
    /**
     * Returns the selected connection speed.
     * 
     * @return See above.
     */
    public int getSpeedLevel() { return speedLevel; }

	/**
	 * Sets to <code>true</code> if the experimenters are administrators,
	 * <code>false</code> otherwise.
	 * 
	 * @param administrator The value to set.
	 */
	public void setAdministrator(Boolean administrator)
	{
		this.administrator = administrator;
	}
	
	/**
	 * Returns <code>true</code> if the experimenter is an administrator,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public Boolean isAdministrator() { return administrator; }

	/**
	 * Sets to <code>true</code> if the experimenter is the owner of a group,
	 * <code>false</code> otherwise.
	 * 
	 * @param owner The value to set.
	 */
	public void setOwner(Boolean owner)
	{
		this.owner = owner;
	}
	
	/**
	 * Returns <code>true</code> if the experimenter is the owner of a group,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public Boolean isOwner() { return owner; }

	/**
	 * Sets to <code>true</code> if the experimenter has to be activated,
	 * <code>false</code> otherwise.
	 * 
	 * @param administrator The value to set.
	 */
	public void setActive(Boolean active)
	{
		this.active = active;
	}
	
	/**
	 * Returns <code>true</code> if the experimenter has to be activated,
	 * <code>false</code> otherwise.
	 * 
	 * @return See above.
	 */
	public Boolean isActive() { return active; }
	
    /**
     * Formats user name and password.
     * Each character of the password is replaced by a star.
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("User Name: ");
        buf.append(userName);
        buf.append(" -- Password: ");
        for (int i = 0; i < password.length(); ++i) buf.append('*');
        return buf.toString();
    }
    
}
