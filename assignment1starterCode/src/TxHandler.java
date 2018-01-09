import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        if(utxoPool == null){
            this.utxoPool = new UTXOPool();
        }else{
            this.utxoPool = new UTXOPool(utxoPool);
        }
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        Map<Integer, Integer> ds = new HashMap<>();

        if(inputs == null || outputs == null){
            return true;
        }

        // all outputs claimed by {@code tx} are in the current UTXO pool,

        ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();
        Transaction.Output out = null;
        Transaction.Input in = null;
        UTXO utxo = null;


        // for each Input
        for(int i = 0; i < tx.numInputs(); i++){
            in = tx.getInput(i);

            // for each Unspent Transaction
            for(int j = 0; j < utxos.size(); j ++){
                utxo = utxos.get(j);
                if(utxo.equals(in)){

                    out = this.utxoPool.getTxOutput(utxo);
                    if(!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature)){
                        return false;
                    }
                    if(!this.utxoPool.contains(utxo)){
                        return false;
                    }
                }
            }

            //double spending
            utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (ds.containsKey(utxo.hashCode())) {
                return false;
            }
            ds.put(utxo.hashCode(), utxo.hashCode());
        }

        // (2) the signatures on each input of {@code tx} are valid,
        Transaction.Output claimedOutput = null;
        PublicKey pubKey = null;
        for (int i = 0; i < tx.numOutputs(); i++){
            in = tx.getInput(i);
            if (in == null){
                continue;
            }
            utxo = new UTXO(in.prevTxHash, in.outputIndex);

            if((claimedOutput = utxoPool.getTxOutput(utxo)) == null){
                return false;
            }

            pubKey = claimedOutput.address;
            byte [] message = tx.getRawDataToSign(i);
            byte [] signature = in.signature;

            if (!Crypto.verifySignature(pubKey, message, signature)) {
                return false;
            }
        }

        // (3) no UTXO is claimed multiple times by {@code tx},
        Transaction.Input input1, input2;
        for(int i = 0; i < inputs.size(); i++){
            for(int j = 0; j < inputs.size(); j++) {
                if(i == j){ //I'm looking at myself :D
                    continue;
                }

                input1 = inputs.get(i);
                input2 = inputs.get(j);

                if (input1.hashCode() == input2.hashCode()){
                    return false;
                }
            }
        }

        // (4) all of {@code tx}s output values are non-negative, and
        double output_value = 0.0;
        for(int i = 0; i < tx.numOutputs(); i ++){
            out = tx.getOutput(i);
            if(out == null){
                continue;
            }

            if(out.value < 0){
                return false;
            }
            output_value += out.value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values;
        // and false otherwise.
        double input_value = 0.0;
        for(int i = 0; i < tx.numInputs(); i ++){
            utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
            out = utxoPool.getTxOutput(utxo);
            if (out== null){
                continue;
            }
            input_value += out.value;
        }
        if(input_value < output_value){
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> result = new ArrayList<>();
        if (utxoPool == null) {
            utxoPool = new UTXOPool();
        }
        for (Transaction tx : possibleTxs) {
            for (int i = 0; i < tx.getOutputs().size(); i++) {
                Transaction.Output output = tx.getOutput(i);
                utxoPool.addUTXO(new UTXO(tx.getHash(), i), output);
            }
            for (int i = 0; i < tx.getInputs().size(); i++) {
                Transaction.Input input = tx.getInput(i);
                UTXO ut = new UTXO(input.prevTxHash, i);
                utxoPool.addUTXO(ut, utxoPool.getTxOutput(ut));
            }
        }
        int index = 0;

        for (Transaction tx : possibleTxs) {
            UTXO utxo = new UTXO(tx.getHash(), index);
            if (isValidTx(tx)) {
                result.add(tx);
                utxoPool.addUTXO(utxo, tx.getOutput(index));
            } else if (tx.getInputs() != null && tx.getInputs().size() != 0) {
                utxoPool.removeUTXO(utxo);
            }
        }
        return result.toArray(new Transaction[]{});
    }
}
