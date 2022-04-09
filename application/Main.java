package bgu.spl.mics.application;

import bgu.spl.mics.application.passiveObjects.Attack;
import bgu.spl.mics.application.passiveObjects.Diary;
import bgu.spl.mics.application.passiveObjects.Ewok;
import bgu.spl.mics.application.passiveObjects.Ewoks;
import bgu.spl.mics.application.services.*;
import com.google.gson.*;

import java.io.*;
import java.util.*;

/**
 * This is the Main class of the application. You should parse the input file,
 * create the different components of the application, and run the system.
 * In the end, you should output a JSON.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Thread> list = new LinkedList<>();
        /**
         * parse the input
         */
        Object obj = JsonParser.parseReader(new FileReader(args[0]));
        JsonObject json = (JsonObject) obj;
        JsonArray jsonAttacks = json.getAsJsonArray("attacks");
        Attack[] attacks = new Attack[jsonAttacks.size()];
        for (int i = 0; i < jsonAttacks.size(); i++) {
            List<Integer> serials = new ArrayList<>();
            JsonObject attackJson = (JsonObject) jsonAttacks.get(i).getAsJsonObject();
            JsonArray jsonSerials = (JsonArray) attackJson.get("serials");
            for (int j = 0; j < jsonSerials.size(); j++) {
                int num = jsonSerials.get(j).getAsInt();
                serials.add(num);
            }
            attacks[i] = new Attack(serials, attackJson.get("duration").getAsInt());
        }

        Ewoks ewok = Ewoks.getInstance();
        int ewoksInput = json.get("Ewoks").getAsInt();
        Ewok[] ewoks = new Ewok[ewoksInput];
        for (int i = 0; i < ewoksInput; i++) {
            ewoks[i] = new Ewok(i + 1, true);
        }
        ewok.load(ewoks);
        /**
         * start running the program
         */
        LeiaMicroservice leiaMicroservice = new LeiaMicroservice(attacks);
        C3POMicroservice c3POMicroservice = new C3POMicroservice();
        HanSoloMicroservice hanSoloMicroservice = new HanSoloMicroservice();
        LandoMicroservice landoMicroservice = new LandoMicroservice(json.get("Lando").getAsLong());
        R2D2Microservice r2D2Microservice = new R2D2Microservice(json.get("R2D2").getAsLong());
        list.add(new Thread(leiaMicroservice, "Leia"));
        list.add(new Thread(hanSoloMicroservice, "HanSolo"));
        list.add(new Thread(c3POMicroservice, "C3PO"));
        list.add(new Thread(r2D2Microservice, "R2D2"));
        list.add(new Thread(landoMicroservice, "Lando"));

        for (Thread thread : list) thread.start();
        for (Thread thread : list) thread.join();
        boolean flag = true;
        /**
         * once all threads terminate we generate the output file
         */
        while (flag) {
            for (Thread t : list) {
                if (t.isAlive())
                    break;
                flag = false;
            }
        }
        /**
         * write the output to json file
         */
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter file = new FileWriter(args[1]);
        gson.toJson(Diary.getInstance(), file);
        file.flush();
        file.close();
    }
}