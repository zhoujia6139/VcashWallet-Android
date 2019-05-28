package com.vcashorg.vcashwallet.api.bean;

import java.util.List;

public class NodeOutputs {
    public long highest_index;
    public long last_retrieved_index;
    public List<NodeOutput> outputs;

    public class NodeOutput{
        public String output_type;
        public String commit;
        public boolean spent;
        public String proof;
        public String proof_hash;
        public long block_height;
        public long mmr_index;
    }
}
