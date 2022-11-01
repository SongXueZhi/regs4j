package experiment;

import experiment.internal.DDOutput;
import experiment.internal.TestRunner;
import org.apache.commons.lang3.RandomUtils;
import utils.DDUtil;
import utils.FuzzUtil;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: sxz
 * @Date: 2022/10/21/16:54
 * @Description:
 */
public class DDExample {
    public static void main(String[] args) throws FileNotFoundException {
//        String path = "detail.log";
//        FileOutputStream puts = new FileOutputStream(path,true);
//        PrintStream out = new PrintStream(puts);
//        System.setOut(out);
        fuzz();

    }
    public static Map<String, DDOutput> fuzzAll(){
        DDContext ddContext = new DDContext();
        FuzzInput fuzzInput = FuzzUtil.createFuzzInput();
        TestRunner testRunner = new TestRunner4Fuzz();

        Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                new ProDD(fuzzInput,testRunner),
                        new ProDDPlus(fuzzInput,testRunner),
                        new ProDDPlusD(fuzzInput,testRunner),
                        new ProDDD(fuzzInput,testRunner),
                        new ProDDPlusM(fuzzInput,testRunner)
                        )
                .start();
        DDOutput proDDOutput = ddOutputHashMap.get(ProDD.class.getName());
        DDOutput proDDPlusOut = ddOutputHashMap.get(ProDDPlus.class.getName());
        DDOutput proDDPlusDOut = ddOutputHashMap.get(ProDDPlusD.class.getName());
        DDOutput proDDDOut = ddOutputHashMap.get(ProDDD.class.getName());
        DDOutput proDDPlusMOut = ddOutputHashMap.get(ProDDPlusM.class.getName());

        System.out.println("\ndd input:");
        System.out.println(fuzzInput.fullSet);
        System.out.println(fuzzInput.relatedMap);
        System.out.println(fuzzInput.criticalChanges);
        System.out.println("proDDOutput: " + proDDOutput.resultIndexList);
        System.out.println("proDDPlusOut: " + proDDPlusOut.resultIndexList);
        System.out.println("proDDPlusDOut: " + proDDPlusDOut.resultIndexList);
        System.out.println("proDDDOut: " + proDDDOut.resultIndexList);
        System.out.println("proDDPlusMOut: " + proDDPlusMOut.resultIndexList);
        return ddOutputHashMap;
    }

    public static void batchTest(){
        int count = 0;
        for(int i = 0; i < 1000; i++){
            System.out.println("\nbatchTest: " + (i+1));
            int setSize = RandomUtils.nextInt(100, 300);
            int relatedNum = RandomUtils.nextInt(50, 100);
            int criticalNum = RandomUtils.nextInt(3, 20);
            FuzzInput fuzzInput = FuzzUtil.createFuzzInput(setSize, relatedNum, criticalNum);
            DDContext ddContext = new DDContext();
            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
//                            new ProDD(fuzzInput,testRunner),
//                            new ProDDPlus(fuzzInput,testRunner),
                            new ProDDPlusD(fuzzInput,testRunner)
//                            new ProDDD(fuzzInput,testRunner),
//                            new ProDDPlusM(fuzzInput,testRunner)
                    )
                    .start();
//            DDOutput proDDOutput = ddOutputHashMap.get(ProDD.class.getName());
//            DDOutput proDDPlusOut = ddOutputHashMap.get(ProDDPlus.class.getName());
            DDOutput proDDPlusDOut = ddOutputHashMap.get(ProDDPlusD.class.getName());
//            DDOutput proDDDOut = ddOutputHashMap.get(ProDDD.class.getName());
//            DDOutput proDDPlusMOut = ddOutputHashMap.get(ProDDPlusM.class.getName());
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);
            System.out.println("set: " + fuzzInput.fullSet.size());
            System.out.println("proDDPlusDOut: " +proDDPlusDOut.resultIndexList.size() + proDDPlusDOut.resultIndexList);
            System.out.println("cc: " + cc.size() + cc);
            if(cc.size() == proDDPlusDOut.resultIndexList.size()){
                count++;
            }else {
                System.out.println("fullSet: " + fuzzInput.fullSet);
                System.out.println("relatedMap: " + fuzzInput.relatedMap);
                System.out.println("criticalChanges: " + fuzzInput.criticalChanges);
                break;
            }
        }
        System.out.println("正确： " + count);
    }

    public static void fuzz() {
        int proDDProDOutCount = 0;
        int proDDProDOutSum = 0;
        int proDDOutCount = 0;
        int proDDOutSum = 0;
        for (int i = 0; i < 100; i++) {

            DDContext ddContext = new DDContext();
            FuzzInput fuzzInput = FuzzUtil.createFuzzInput();
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);

            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDDProD(fuzzInput, testRunner),
                            new ProDD(fuzzInput, testRunner)
                    )
                    .start();
            DDOutputWithLoop ProDDProDDOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDProD.class.getName());
            DDOutputWithLoop ProDDOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDD.class.getName());

            proDDProDOutSum += ProDDProDDOut.loop;
            proDDOutSum += ProDDOut.loop;

            if (cc.size() == ProDDProDDOut.resultIndexList.size()) {
                proDDProDOutCount++;
            }
            if (cc.size() == ProDDOut.resultIndexList.size()) {
                proDDOutCount++;
            }

            System.out.println("ProDDProDDOut " + ProDDProDDOut.resultIndexList.size() + " " + ProDDProDDOut.resultIndexList);
            System.out.println("ProDDOut " + ProDDOut.resultIndexList.size() + " " + ProDDOut.resultIndexList);

        }
        System.out.println("\nproDDProDOutCount: " + proDDProDOutCount);
        System.out.println("proDDProDOutSum: " + proDDProDOutSum);
        System.out.println("\nproDDOutCount: " + proDDOutCount);
        System.out.println("proDDOutSum: " + proDDOutSum);
    }

    public static void fuzzProbDD() {
        for (int i = 0; i < 100000; i++) {
            DDContext ddContext = new DDContext();
            int setSize = RandomUtils.nextInt(20, 1000);
            int criticalNum = RandomUtils.nextInt(3, 20);
            FuzzInput fuzzInput = FuzzUtil.createFuzzInput(setSize, 0, criticalNum);

            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDD(fuzzInput, testRunner)
                    )
                    .start();
            DDOutput ProDDOut = ddOutputHashMap.get(ProDD.class.getName());
            System.out.println("i" + i);

            if (fuzzInput.criticalChanges.size() != ProDDOut.resultIndexList.size()) {
                System.out.println("\ndd input:");
                System.out.println(fuzzInput.fullSet);
                System.out.println(fuzzInput.criticalChanges);
                System.out.println("proDDPlusMOut: " + ProDDOut.resultIndexList.size() + ProDDOut.resultIndexList);
                break;
            }
        }
    }

}
