import java.security.PublicKey;
import java.util.*;

public class MaxFeeTxHandler {

    UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        if(utxoPool!=null){
            this.utxoPool=new UTXOPool(utxoPool);
        }else{
            this.utxoPool=new UTXOPool();
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

        if(inputs == null || outputs == null){
            return false;
        }

        // (1) all outputs claimed by {@code tx} are in the current UTXO pool,
        ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();
        Transaction.Output out = null;
        Transaction.Input in = null;
        UTXO utxo = null;

        // (2) the signatures on each input of {@code tx} are valid,
        Transaction.Output claimedOutput;
        PublicKey pubKey;
        for (int i = 0; i < tx.numInputs(); i++){
            in = tx.getInput(i);
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
        Set<UTXO> UTXO_set = new HashSet<>();
        for (int i = 0; i < tx.numInputs(); i++){
            in = tx.getInput(i);
            utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (UTXO_set.contains(utxo)) {
                return false;
            }
            UTXO_set.add(utxo);
        }

        // (4) all of {@code tx}s output values are non-negative, and
        double output_value = 0.0;
        for(int i = 0; i < tx.numOutputs(); i ++){
            out = tx.getOutput(i);

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
        ArrayList<Transaction> validedTarnsactions = new ArrayList<Transaction>();
        if(possibleTxs == null || possibleTxs.length == 0){
            return null;
        }

        for(Transaction tx:possibleTxs){
            boolean valid = this.isValidTx(tx);

            if(valid){
                double fee = getFee(tx);
                if(fee > 0d){
                    validedTarnsactions.add(tx);
                    updateUTXO(tx);
                }
            }
        }

        Transaction[] ret = new Transaction[validedTarnsactions.size()];
        for(int i=0; i < validedTarnsactions.size(); i++){
            ret[i] = validedTarnsactions.get(i);
        }
        return ret;
    }

    private void updateUTXO(Transaction tx){
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();
        for(Transaction.Input input:inputs){
            UTXO tmp = new UTXO(input.prevTxHash, input.outputIndex);
            for(UTXO utxo:utxos){
                if(utxo.equals(tmp)){
                    this.utxoPool.removeUTXO(utxo);
                    break;
                }
            }
        }

        for(int i = 0; i < outputs.size(); i++){
            Transaction.Output output=outputs.get(i);
            UTXO utxo = new UTXO(tx.getHash(), i);
            this.utxoPool.addUTXO(utxo, output);
        }

    }

    private double getFee(Transaction tx){
        double inVal = 0d, outVal = 0d;
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();
        for(int i=0; i < inputs.size(); i++){
            Transaction.Input input = inputs.get(i);
            UTXO in = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = null;
            for(UTXO utxo:utxos){
                if(utxo.equals(in)){
                    output = this.utxoPool.getTxOutput(utxo);
                    inVal += output.value;
                }
            }
        }

        for(Transaction.Output out:outputs){
            outVal+=out.value;
        }

        return inVal-outVal;
    }
}