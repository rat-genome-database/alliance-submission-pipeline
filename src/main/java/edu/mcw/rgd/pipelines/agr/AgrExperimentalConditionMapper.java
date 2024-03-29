package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.process.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class AgrExperimentalConditionMapper {

    static private AgrExperimentalConditionMapper _instance = new AgrExperimentalConditionMapper();

    private AgrExperimentalConditionMapper() {

    }

    public static AgrExperimentalConditionMapper getInstance() {
        return _instance;
    }

    private HashMap<String, Info> data;

    synchronized public Info getInfo(String acc) {

        try {
            lazyLoadInfo();
        } catch( IOException e) {
            throw new RuntimeException(e);
        }

        return data.get(acc);
    }

    void lazyLoadInfo() throws IOException {

        if( data!=null ) {
            return;
        }

        data = new HashMap<>();

        BufferedReader in = Utils.openReader("data/AGR_experimental_condition_mappings.txt");
        String line;
        while( (line=in.readLine())!=null ) {
            if( line.startsWith("#") ) {
                continue;
            }
            String[] cols = line.split("[\\t]", -1);
            Info info = new Info();
            info.xcoAcc = cols[0];
            info.xcoTerm = cols[1];
            info.zecoAcc = cols[2];
            info.zecoTerm = cols[3];

            // in March 2023, The Alliance will autogenerate 'condition_summary';
            // thus to avoid duplicate summaries, generation of 'condition_free_text' must be suppressed
            /*
            if( cols.length>4 && !Utils.isStringEmpty(cols[4]) ) {
                info.conditionStatement = cols[4].replace("\"", "");
            } else {
                info.conditionStatement = info.zecoTerm+":"+info.xcoTerm.replace("\"", "");
            }
            */

            data.put(info.xcoAcc, info);
        }
        in.close();
    }

    public class Info {
        public String xcoAcc;
        public String xcoTerm;
        public String zecoAcc;
        public String zecoTerm;
        public String conditionStatement;
    }
}
