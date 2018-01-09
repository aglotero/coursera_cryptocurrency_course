import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class handleTxsTest {


    @Test
    public void handleTxsWithValidTransactions()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
                .setPeopleSize(10)
                .setUtxoTxNumber(10)
                .setMaxUtxoTxOutput(10)
                .setMaxValue(200)
                .setTxPerTest(10)
                .setMaxInput(10)
                .setMaxOutput(10)
                .setCorruptedPercentage(0) // All valid transactions
                .build();

        assertTestSetIsValid(utxoTestSet);
    }

    @Test
    public void handleTxsWithInvalidTransactions()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
                .setPeopleSize(10)
                .setUtxoTxNumber(10)
                .setMaxUtxoTxOutput(10)
                .setMaxValue(200)
                .setTxPerTest(10)
                .setMaxInput(10)
                .setMaxOutput(10)
                .setCorruptedPercentage(0.50) // Half invalid
                .build();

        assertTestSetIsValid(utxoTestSet);
    }

    @Test
    public void handleTxsWithInvalidTransactionsWrongSignature()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
                .setPeopleSize(10)
                .setUtxoTxNumber(10)
                .setMaxUtxoTxOutput(10)
                .setMaxValue(200)
                .setTxPerTest(10)
                .setMaxInput(10)
                .setMaxOutput(10)
                .setClaimingOutputsNotInPool(true)
                .setCorruptedPercentage(0.50) // Half invalid
                .build();

        assertTestSetIsValid(utxoTestSet);
    }

    private static void assertTestSetIsValid(final UtxoTestSet utxoTestSet) {
        final ValidationLists<Transaction> trxsValidation = utxoTestSet.getValidationLists();

        // Instantiate student solution
        final TxHandler txHandler = new TxHandler(utxoTestSet.getUtxoPool());

        ArrayList<Transaction> testTrans = new ArrayList<Transaction>();

        // Check validation of all the transactions in the set
        for (Transaction tx: trxsValidation.allElements()) {
            assertEquals(txHandler.isValidTx(tx), trxsValidation.isValid(tx) );
            if(txHandler.isValidTx(tx)) {
                testTrans.add(tx);
            }
        }

        Transaction[] result = txHandler.handleTxs(testTrans.toArray(new Transaction[]{}));

        assertEquals(testTrans.size(), result.length);
    }


}
