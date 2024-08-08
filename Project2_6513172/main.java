
package Project2_6513172;
//Puthipong Yomabut 6513134
// Phattaradanai Sornsawang 6513172
// Patiharn Kamenkit 6513170
//Praphasiri wannawong 6513116
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

class Material {
    private String name;
    private int balance = 0;

    public Material(String n) {
        this.name = n;
    }

    synchronized public void add(int amount) {
        balance += amount;
    }


    synchronized public int  retrieve(int amount) {
        if (balance >= amount) {
            balance -= amount;
            return amount;
        } else {
            int retrievedAmount = balance;
            balance = 0;
            return retrievedAmount;
        }
    }

    synchronized public String getName() {
        return name;
    }

    public int getBalance() {
        return balance;
    }

}

class SupplierThread extends Thread{
    private int[] dailysup;
    private ArrayList<Material> material;
    private CyclicBarrier       cb;
    public SupplierThread(String n, int[] ds, CyclicBarrier cb) {
        super(n);
        this.dailysup = ds;
        this.cb = cb;
    }

    public void setMaterial(ArrayList<Material> material){
        this.material = material;
    }


    @Override
    synchronized public void run() {
        Thread.currentThread().setName(this.getName());
        synchronized (material) {
            for (int i = 0; i < material.size(); i++) {
                System.out.printf("%s  >>   put %7d %s ", Thread.currentThread().getName(), dailysup[i], material.get(i).getName());
                material.get(i).add(dailysup[i]);
                System.out.printf("     balance = %4d %s%n", material.get(i).getBalance(), material.get(i).getName());
            }
        }
        try {
            cb.await();
        } catch (InterruptedException e) {
        } catch (BrokenBarrierException e) {
        }
    }
}

class FactoryThread extends Thread {
    private String product;
    private int lotsproduct;
    private int[] dailyproduct;
    private int[] holding;
    private int[] amountToRetrieve;

    private ArrayList<Material> material;
    private CyclicBarrier       cb;

    public FactoryThread(String n, String p, int[] l, CyclicBarrier cb) {
        super(n);

        this.product = p;
        this.dailyproduct = l;
        amountToRetrieve = new int[dailyproduct.length];
        this.cb=cb;
        this.holding = new int[l.length];
        for(int i =0;i<holding.length;i++){
            holding[i]=0;;
        }
    }

    public void setMaterial(ArrayList<Material> material){
        this.material = material;
    }

    public String getProduct() {
        return product;
    }
    public int getLotsproduct(){
        return lotsproduct;
    }

    @Override
    synchronized public void run() {
        synchronized (material) {
            Thread.currentThread().setName(this.getName());
            System.out.printf("%-11s >>   Holding", this.getName());
            for (int i = 0; i < material.size(); i++) {
                System.out.printf("  %4d %-7s     ", holding[i], material.get(i).getName());
            }
            System.out.println();
        }
        try {
            cb.await();
        } catch (InterruptedException e) {
        } catch (BrokenBarrierException e) {
        }
        for (int i = 0; i < material.size(); i++) {
            synchronized (material) {
                //material.get(i).retrieve(dailyoneproduct[i]);
                if (holding[i] != dailyproduct[i]) {
                    amountToRetrieve[i] = material.get(i).retrieve(dailyproduct[i]);
                    System.out.printf("%-7s   >>   get    %4d %-7s ", Thread.currentThread().getName(), amountToRetrieve[i], material.get(i).getName());
                    System.out.printf("     balance = %4d %-7s", material.get(i).getBalance(), material.get(i).getName());
                    System.out.println();
                }
            }
        }
        try {
            cb.await();
        } catch (InterruptedException e) {}
        catch (BrokenBarrierException e){}
        boolean canProduce = true;
        for(int i = 0; i < material.size();i ++) {
            if ( amountToRetrieve[i] < dailyproduct[i]){
                canProduce = false;
                break;
            }
        }
        synchronized (material) {
            if (canProduce) {
                lotsproduct++;
                System.out.printf("%-9s   >>   %-9s production succeeds, lot %d", Thread.currentThread().getName(), product, lotsproduct);
                System.out.println();
                for (int i = 0; i < material.size(); i++) {
                    holding[i]=0;
                }
            } else {
                System.out.printf("%-9s   >>   %-9s production failed", Thread.currentThread().getName(), product);
                System.out.println();
                for (int i = 0; i < material.size(); i++) {
                    if (amountToRetrieve[i] < dailyproduct[i] && amountToRetrieve[i] > 0) {
                        System.out.printf("%-7s   >>   put    %4d %s  ", Thread.currentThread().getName(), amountToRetrieve[i], material.get(i).getName());
                        System.out.printf("    balance = %4d %-7s", amountToRetrieve[i], material.get(i).getName());
                        System.out.println();
                        if (amountToRetrieve[i] < dailyproduct[i]) {
                            material.get(i).add(amountToRetrieve[i]);
                        }
                    } else if (amountToRetrieve[i] == dailyproduct[i] && dailyproduct[i] != holding[i]) {
                        holding[i] += amountToRetrieve[i];
                    }
                }
            }
        }
    }
}

public class main {
    public static void main(String[] args) {
        int checkline = 0;
        int day = 0;
        int lot = 0;
        int[] dailysupply = null;
        int[] dailyfactory = null;
        int[] holding = null;
        CyclicBarrier barrier = new CyclicBarrier(3);
        CyclicBarrier barrier2 = new CyclicBarrier(2);
        String path = "src/main/java/Project2/";
        String file = "config.txt";

        ArrayList<Material> materialAL = new ArrayList<>();
        ArrayList<SupplierThread> dailysupplyAl = new ArrayList<>();
        ArrayList<FactoryThread> dailyfactoryAl = new ArrayList<>();
        boolean opensuccess = false;
        while (!opensuccess) {
            try (Scanner scan = new Scanner(new File(path + file))) {
                opensuccess = true;
                System.out.println(Thread.currentThread().getName() + " >> read configs from " + path + file + "\n");
                while (scan.hasNext()) {
                    String line = scan.nextLine();
                    String[] buf = line.split(",");
                    if (checkline == 0) {
                        day = Integer.parseInt(buf[1].trim());
                        System.out.printf("%-6s \t    >>   simulation days = %d\n", Thread.currentThread().getName(), day);
                        checkline++;
                    } else if (checkline == 1) {
                        for (int i = 1; i < buf.length; i++) {
                            materialAL.add(new Material(buf[i].trim()));
                        }
                        checkline++;
                    } else {
                        if (buf[0].trim().equals("S")) {
                            System.out.printf("%-6s \t    >>  %-15s daily supply rates =  ", Thread.currentThread().getName(), buf[1]);
                            dailysupply = new int[buf.length - 2];
                            for (int i = 0; i < buf.length - 2; i++) {
                                dailysupply[i] = Integer.parseInt(buf[i + 2].trim());
                                System.out.printf("%3d %-10s", dailysupply[i], materialAL.get(i).getName());
                            }
                            System.out.println();
                            dailysupplyAl.add(new SupplierThread(buf[1].trim(), dailysupply,barrier2));
                            checkline++;
                        } else if (buf[0].trim().equals("F")) {
                            System.out.printf("%-6s \t    >>  %-15s daily use =  ", Thread.currentThread().getName(), buf[1]);
                            dailyfactory = new int[buf.length - 4];
                            holding = new int[buf.length - 4];
                            for (int i = 0; i < buf.length - 4; i++) {
                                lot = Integer.parseInt(buf[3].trim());
                                dailyfactory[i] = Integer.parseInt(buf[i + 4].trim()) * lot;
                                System.out.printf("%3d %-10s   ", dailyfactory[i], materialAL.get(i).getName());
                            }
                            System.out.printf("producing %5d %-10s", lot, buf[2].trim());
                            dailyfactoryAl.add(new FactoryThread(buf[1].trim(), buf[2].trim(), dailyfactory,barrier));
                            System.out.println();
                            checkline++;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println(e);
                System.out.println("Thread " + Thread.currentThread().getName() + " >> Enter config file for simulation = ");
                file = new Scanner(System.in).next();
            }
        }

        // Set material
        for(FactoryThread thread : dailyfactoryAl){
            thread.setMaterial(materialAL);
        }
        for(SupplierThread thread : dailysupplyAl){
            thread.setMaterial(materialAL);
        }

        for (int i = 0; i < day; i++) {

            ExecutorService Supplier = Executors.newFixedThreadPool(dailysupplyAl.size());
            ExecutorService Factory = Executors.newFixedThreadPool(dailyfactoryAl.size());

            // Shutdown the thread pool when you're done with it
            System.out.printf(Thread.currentThread().getName() + "        >> ");
            System.out.println("-".repeat(90));
            System.out.printf(Thread.currentThread().getName() + "        >>   Day %d\n", i + 1);
            // Start Supplier Thread
            for (SupplierThread thread : dailysupplyAl) {
                Supplier.submit(thread);
            }
            Supplier.shutdown();
            try {
                Supplier.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (FactoryThread thread : dailyfactoryAl) {
                Factory.submit(thread);
            }
            Factory.shutdown();
            try {
                Factory.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (SupplierThread thread : dailysupplyAl) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (FactoryThread thread : dailyfactoryAl) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<FactoryThread> sortedFactoryThreads = dailyfactoryAl.stream()
                .sorted((t1, t2) -> Integer.compare(t2.getLotsproduct(), t1.getLotsproduct()))
                .toList();

        System.out.printf("%-7s     >> ",Thread.currentThread().getName());
        System.out.print("-".repeat(99));
        System.out.println();
        System.out.printf("%-7s     >>   Summary",Thread.currentThread().getName());
        System.out.println();

        for (FactoryThread thread : sortedFactoryThreads) {
            System.out.printf("%-7s     >>   Total %-9s = %3d lots", Thread.currentThread().getName(), thread.getProduct(), thread.getLotsproduct());
            System.out.println();
        }
    }
}