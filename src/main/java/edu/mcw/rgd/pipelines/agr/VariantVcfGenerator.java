package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VariantVcfGenerator {

    private Dao dao;

    Logger log = LogManager.getLogger("status");

    public void run() {


    }


    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }
}
