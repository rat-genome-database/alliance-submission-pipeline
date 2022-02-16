package edu.mcw.rgd.pipelines.agr;


import edu.mcw.rgd.dao.AbstractDAO;

/**
 * @author mtutaj
 * @since 2/16/2022
 * All database code lands here
 */
public class Dao {

    private AbstractDAO dao = new AbstractDAO();

    public String getConnectionInfo() {
        return dao.getConnectionInfo();
    }

}
