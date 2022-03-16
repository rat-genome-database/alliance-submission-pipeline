package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mtutaj on 5/30/2017
 */
public class Main {

    private String version;
    private Dao dao;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main main = (Main) (bf.getBean("main"));

        try {
            main.run(args, bf);
        } catch(Exception e) {
            Utils.printStackTrace(e, main.log);
            throw e;
        }
    }

    void run(String[] args, DefaultListableBeanFactory bf) throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());

        Date dateStart = new Date();
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("  started at: "+sdt.format(dateStart));
        log.info("  "+dao.getConnectionInfo());
        log.info("===");

        for( String arg: args ) {
            switch (arg){
                case "--variant-vcf-generator": {
                    VariantVcfGenerator g = (VariantVcfGenerator) (bf.getBean("variantVcfGenerator"));
                    g.run();
                }
                case "--curation-daf-generator": {
                    CurationDafGenerator d = (CurationDafGenerator) (bf.getBean("curationDafGenerator"));
                    d.run();
                }
            }
        }

        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
        log.info("");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }
}
