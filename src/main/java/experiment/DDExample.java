package experiment;

import experiment.internal.DDOutput;
import experiment.internal.TestRunner;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.RandomUtils;
import utils.DDUtil;
import utils.FuzzUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

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
//        fuzzProbDDPlusM();
        DDContext ddContext = new DDContext();
        TestRunner testRunner = new TestRunner4Fuzz();
        FuzzInput fuzzInput = new FuzzInput();

        List<Integer> set= new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            set.add(i);
        }
        MultiValuedMap<Integer, Integer> relatedMap = new ArrayListValuedHashMap<>(3);
        relatedMap.put(5, 3);
        relatedMap.put(9, 4);
        relatedMap.put(4, 9);
        Set<Integer> criticalChanges = new HashSet<>(3);
        //Note that! there may be 1 cc(when c1 equals c2) or 2 cc.
        for (int i = 0; i < 3; i++) {
            criticalChanges.add(i);
        }

        fuzzInput.fullSet = set;
        fuzzInput.relatedMap = relatedMap;
        fuzzInput.criticalChanges = criticalChanges;

        System.out.println("\ndd input:");
        System.out.println(fuzzInput.fullSet);
        System.out.println(fuzzInput.relatedMap);
        System.out.println(fuzzInput.criticalChanges);
        Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDD(fuzzInput,testRunner)).start();
            DDOutput proDDOutput = ddOutputHashMap.get(ProDD.class.getName());
        System.out.println("proDDOutput: " + proDDOutput.resultIndexList);
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
        int ProDDPlusMOutCount = 0;
        int ProDDPlusMOutSum = 0;
        double ProDDPlusMOutError = 0;
        int ProDDPlusnnOutCount = 0;
        int ProDDPlusnnOutSum = 0;
        double ProDDPlusnnOutError = 0;
        int ProDDPlusnlognOutCount = 0;
        int ProDDPlusnlognOutSum = 0;
        double ProDDPlusnlognOutError = 0;
        int ProDDOutCount = 0;
        int ProDDOutSum = 0;
        double ProDDOutError = 0;
        int ddminOutCount = 0;
        int ddminOutSum = 0;
        double ddminOutError = 0;

        for (int i = 0; i < 100; i++) {
            DDContext ddContext = new DDContext();
            int setSize = RandomUtils.nextInt(8, 8);
            int relatedNum = RandomUtils.nextInt(1, 3);
            int criticalNum = RandomUtils.nextInt(1, 4);
            FuzzInput fuzzInput = FuzzUtil.createFuzzInput(setSize, relatedNum, criticalNum);
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);

//            System.out.println("\n" + i + " dd input:");
//            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
//            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
//            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
//            System.out.println("cc " + cc.size() + " " + cc);
            //System.out.println("dependencies " + fuzzInput.dependencies.size() + " " + fuzzInput.dependencies);

            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
//                            new ProDDPlus(fuzzInput, testRunner),
                            new ProDD(fuzzInput, testRunner)
//                            ,
//                            new ddmin(fuzzInput, testRunner),
//                            new ProDDPlus_nn(fuzzInput, testRunner),
//                            new ProDDPlus_nlogn(fuzzInput, testRunner)
                            )
                    .start();

//            DDOutputWithLoop ProDDPlusMOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDPlus.class.getName());
//            ProDDPlusMOutSum += ProDDPlusMOut.loop;
//            ProDDPlusMOutError += (double)ProDDPlusMOut.resultIndexList.size() / cc.size();
//            if (cc.size() == ProDDPlusMOut.resultIndexList.size()) {
//                ProDDPlusMOutCount++;
//            }
//
//            DDOutputWithLoop ProDDPlusnnOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDPlus_nn.class.getName());
//            ProDDPlusnnOutSum += ProDDPlusnnOut.loop;
//            ProDDPlusnnOutError += (double)ProDDPlusnnOut.resultIndexList.size() / cc.size();
//            if (cc.size() == ProDDPlusnnOut.resultIndexList.size()) {
//                ProDDPlusnnOutCount++;
//            }
//
//            DDOutputWithLoop ProDDPlusnlognOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDPlus_nlogn.class.getName());
//            ProDDPlusnlognOutSum += ProDDPlusnlognOut.loop;
//            ProDDPlusnlognOutError += (double)ProDDPlusnlognOut.resultIndexList.size() / cc.size();
//            if (cc.size() == ProDDPlusnlognOut.resultIndexList.size()) {
//                ProDDPlusnlognOutCount++;
//            }

            DDOutputWithLoop ProDDOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDD.class.getName());
            ProDDOutSum += ProDDOut.loop;
            ProDDOutError += (double)ProDDOut.resultIndexList.size() / cc.size();
            if (cc.size() == ProDDOut.resultIndexList.size()) {
                ProDDOutCount++;
            }
            if (cc.size() < ProDDOut.resultIndexList.size()-1) {
                ProDDOutCount++;


//            DDOutputWithLoop ddminOut = (DDOutputWithLoop) ddOutputHashMap.get(ddmin.class.getName());
//            ddminOutSum += ddminOut.loop;
//            ddminOutError += (double)ddminOut.resultIndexList.size() / cc.size();
//            if (cc.size() == ddminOut.resultIndexList.size()) {
//                ddminOutCount++;
//            }

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);
            //System.out.println("dependencies " + fuzzInput.dependencies.size() + " " + fuzzInput.dependencies);
//            System.out.println("ddminOut " + ddminOut.resultIndexList.size() + " " + ddminOut.resultIndexList);
            System.out.println("ProDDOut " + ProDDOut.resultIndexList.size() + " " + ProDDOut.resultIndexList);
//            System.out.println("ProDDPlusOut " + ProDDPlusMOut.resultIndexList.size()  + " " + ProDDPlusMOut.resultIndexList);
//            System.out.println("ProDDPlusnnOut " + ProDDPlusnnOut.resultIndexList.size()  + " " + ProDDPlusnnOut.resultIndexList);
//            System.out.println("ProDDPlusnlognOut " + ProDDPlusnlognOut.resultIndexList.size()  + " " + ProDDPlusnlognOut.resultIndexList);
            }
        }
//        System.out.println("\nddminOutCount: " + ddminOutCount);
//        System.out.println("ddminOutSum: " + ddminOutSum);
//        System.out.println("ddminOutError: " + ddminOutError);

        System.out.println("\nproDDOutCount: " + ProDDOutCount);
        System.out.println("proDDOutSum: " + ProDDOutSum);
        System.out.println("proDDOutError: " + ProDDOutError);

//        System.out.println("\nproDDPlusOutCount: " + ProDDPlusMOutCount);
//        System.out.println("proDDPlusOutSum: " + ProDDPlusMOutSum);
//        System.out.println("proDDPlusOutError: " + ProDDPlusMOutError);
//
//        System.out.println("\nproDDPlusnnOutCount: " + ProDDPlusnnOutCount);
//        System.out.println("proDDPlusnnOutSum: " + ProDDPlusnnOutSum);
//        System.out.println("proDDPlusnnOutError: " + ProDDPlusnnOutError);
//
//        System.out.println("\nproDDPlusnlognOutCount: " + ProDDPlusnlognOutCount);
//        System.out.println("proDDPlusnlognOutSum: " + ProDDPlusnlognOutSum);
//        System.out.println("proDDPlusnlognOutError: " + ProDDPlusnlognOutError);
    }

    public static void fuzzddmin() {
        int ddminOutCount = 0;
        int ddminOutSum = 0;
        for (int i = 0; i < 1; i++) {
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
                            new ddmin(fuzzInput, testRunner)
                    )
                    .start();
            DDOutputWithLoop ddminOut = (DDOutputWithLoop) ddOutputHashMap.get(ddmin.class.getName());

            ddminOutSum += ddminOut.loop;
            if (cc.size() == ddminOut.resultIndexList.size()) {
                ddminOutCount++;
            }

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);
            System.out.println("ProDDPlusMminiOut " + ddminOut.resultIndexList.size() + " " + ddminOut.resultIndexList);
        }

        System.out.println("\nddminOutCount: " + ddminOutCount);
        System.out.println("ddminOutSum: " + ddminOutSum);

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

    public static void fuzzProbDDPlus() {
        int ProDDPlusOutCount = 0;
        int ProDDPlusOutSum = 0;
        double ProDDPlusOutError = 0;

        for (int i = 0; i < 100; i++) {
            DDContext ddContext = new DDContext();
            int setSize = RandomUtils.nextInt(2, 50);
            int relatedNum = RandomUtils.nextInt(0, (setSize / 2) + 1);
            int criticalNum = RandomUtils.nextInt(3, 20);
            FuzzInput fuzzInput = FuzzUtil.createFuzzInput(setSize, relatedNum, 2);
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);

            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDDPlus(fuzzInput, testRunner)
                    )
                    .start();
            DDOutputWithLoop ProDDPlusOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDPlus.class.getName());

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);
            System.out.println("ProDDPlusMOut " + ProDDPlusOut.resultIndexList.size() + " " + ProDDPlusOut.resultIndexList);
            System.out.println("ProDDPlusMOut loop: " + ProDDPlusOut.loop );
            System.out.println("proDDPlusOutError: " + (double)ProDDPlusOut.resultIndexList.size() / cc.size());


            ProDDPlusOutSum += ProDDPlusOut.loop;
            ProDDPlusOutError += (double)ProDDPlusOut.resultIndexList.size() / cc.size();
            if (cc.size() == ProDDPlusOut.resultIndexList.size()) {
                ProDDPlusOutCount++;
            }
        }
        System.out.println("\nProDDPlusOutCount: " + ProDDPlusOutCount);
        System.out.println("proDDPlusOutSum: " + ProDDPlusOutSum);
        System.out.println("proDDPlusOutError: " + ProDDPlusOutError);
    }

    public static void fuzzProbDDPlusM() {
        for (int i = 0; i < 1; i++) {
            DDContext ddContext = new DDContext();
            //FuzzInput fuzzInput = FuzzUtil.createFuzzInput(5,2,2);
            FuzzInput fuzzInput = FuzzUtil.createFuzzInputWithDependencies(7,7,1,2,false);
            List<Integer> cc = new ArrayList<Integer>(fuzzInput.criticalChanges);
            DDUtil.getTestSetWithDependency(cc, fuzzInput.relatedMap);

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);
            System.out.println("dependencies " + fuzzInput.dependencies.size() + " " + fuzzInput.dependencies);

            TestRunner testRunner = new TestRunner4Fuzz();

            Map<String, DDOutput> ddOutputHashMap = ddContext.addDDStrategy(
                            new ProDDPlusM(fuzzInput, testRunner)
                    )
                    .start();
            DDOutputWithLoop ProDDPlusMOut = (DDOutputWithLoop) ddOutputHashMap.get(ProDDPlusM.class.getName());

            System.out.println("\n" + i + " dd input:");
            System.out.println("fullSet " + fuzzInput.fullSet.size() + " " + fuzzInput.fullSet);
            System.out.println("relatedMap " + fuzzInput.relatedMap.size() + " " + fuzzInput.relatedMap);
            System.out.println("criticalChanges " + fuzzInput.criticalChanges.size() + " " + fuzzInput.criticalChanges);
            System.out.println("cc " + cc.size() + " " + cc);
            System.out.println("dependencies " + fuzzInput.dependencies.size() + " " + fuzzInput.dependencies);
            System.out.println("ProDDPlusMOut " + ProDDPlusMOut.resultIndexList.size() + " " + ProDDPlusMOut.resultIndexList);
            System.out.println("ProDDPlusMOut loop: " + ProDDPlusMOut.loop );

            if (cc.size() != ProDDPlusMOut.resultIndexList.size()) {
                break;
            }

        }
    }
}
