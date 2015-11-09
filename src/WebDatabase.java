/**
 * Created by jingxiaogu on 11/1/15.
 */
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.json.*;

public class WebDatabase {
    private String accountKeyEnc;
    private String site;
    private String prefix;
    private String suffix;
    private double t_es;
    private double t_ec;
    public WebDatabase (String accountKey, double es, double ec, String host){
        accountKeyEnc = new String(Base64.encodeBase64((accountKey + ":" + accountKey).getBytes()));
        t_es = es;
        t_ec = ec;
        site = host;
        prefix =  "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Composite?Query=%27site%3a" + site + "%20";
        suffix = "%27&$top=4&$format=json";
    }


    private ArrayList<String> classification() throws IOException {
        System.out.println("Classifying...");

        HashMap<String, String> mapParent = new HashMap<String, String>();//Parent HashMap
        mapParent.put("Computers", "Root"); mapParent.put("Health", "Root"); mapParent.put("Sports", "Root");
        mapParent.put("Hardware", "Computers"); mapParent.put("Programming", "Computers"); mapParent.put("Diseases", "Health");
        mapParent.put("Fitness", "Health"); mapParent.put("Soccer", "Sports"); mapParent.put("Basketball", "Sports");

        HashMap<String, ArrayList<String>> mapChild = new HashMap<String, ArrayList<String>>();//Child HashMap
        mapChild.put("Root", new ArrayList<String>(Arrays.asList("Computers", "Health", "Sports")));
        mapChild.put("Computers", new ArrayList<String>(Arrays.asList("Hardware", "Programming")));
        mapChild.put("Health", new ArrayList<String>(Arrays.asList("Diseases", "Fitness")));
        mapChild.put("Sports", new ArrayList<String>(Arrays.asList("Soccer", "Basketball")));

        HashMap<String, Double> mapCoverage = new HashMap<String, Double>();//Coverage HashMap
        HashMap<String, Double> mapSpecificity = new HashMap<String, Double>();//Specificity HashMap
        String[] categoryList = {"Computers", "Health", "Sports", "Hardware", "Programming", "Diseases", "Fitness", "Soccer", "Basketball"};

        //calculates the coverage for each category
        for (int i = 0; i < categoryList.length; i++) {
            String category = categoryList[i];
//            String path =  WebDatabase.class.getResource("WebDatabase.class").toString();
//            path = path.substring(5, path.length() - "WebDatabase.class".length() - 1) + "/classification/" + category + ".txt";
            String path = System.getProperty("user.dir") + "/classification/" + category + ".txt";
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            double ec = 0;
            while ((line = br.readLine()) != null) {
                String[] sArray = line.trim().split("\\s+");
                String query = new String();
                for (int j = 0; j < sArray.length; j++) {
                    if (j > 0) query += (sArray[j] + "%20");
                }
                query = query.substring(0, query.length() - 3);
                String bingUrl = prefix + query + suffix;
                ec += getMatches(bingUrl);
            }
            mapCoverage.put(category, ec);
        }

        //calculate the specificity of each category
        for (int i = 0; i < categoryList.length; i++) {
            String category = categoryList[i];
            String parent = mapParent.get(category);
            double numer;
            if (parent.equals("Root")) numer = mapCoverage.get(category);
            else numer = mapCoverage.get(category) * mapSpecificity.get(parent);
            double denomi = 0;
            ArrayList<String> childList = mapChild.get(parent);
            for (int j = 0; j < childList.size(); j++) {
                String child = childList.get(j);
                denomi += mapCoverage.get(child);
            }
            double es = numer / denomi;
            mapSpecificity.put(category, es);
        }

        //print coverage and specificity for each category
        String[] strArray = {"Computers", "Hardware", "Programming",  "Health", "Diseases", "Fitness", "Sports", "Basketball", "Soccer"};
        for (int i = 0; i < strArray.length; i++) {
            String category = strArray[i];
            System.out.println("Specificity for category: " + category + " is " + mapSpecificity.get(category));
            System.out.println("Coverage for category: " + category + " is " +  mapCoverage.get(category).longValue());
        }

        System.out.println();
        System.out.println();

        ArrayList<String> classiRes = new ArrayList<String>();
        ArrayList<String> tmp = new ArrayList<String>();
        String[] str = {"Hardware", "Programming", "Computers", "Diseases", "Fitness", "Health", "Basketball", "Soccer", "Sports"};
        System.out.println("classification");

        //Output and save the result
        for (int i = 0; i < str.length; i++) {
            String category = str[i];
            double coverage = mapCoverage.get(category);
            double specificity = mapSpecificity.get(category);
            if (coverage > t_ec && specificity > t_es && (!mapChild.containsKey(category) || !isInList(tmp, mapChild.get(category)))) {
                tmp.add(category);
                if (category.equals("Computers") || category.equals("Health") || category.equals("Sports")) {
                    classiRes.add(category);
                    System.out.println("Root/" + category);
                }
                else {
                    if (!classiRes.contains(mapParent.get(category))) classiRes.add(mapParent.get(category));
                    System.out.println("Root/" + mapParent.get(category) + "/" + category);
                }
            }
        }
        if (classiRes.size() == 0) System.out.println("Root");
        System.out.println();
        classiRes.add("Root");

        return classiRes;

    }

    private void summaryConstruction(ArrayList<String> categoryList) throws IOException{
        TreeMap<String, ArrayList<String>> cq = new TreeMap<String, ArrayList<String>>();//TreeMap (category, query)

        ArrayList<String> rl = new ArrayList<String>();


        //put category and query into TreeMap
        for (int i = 0; i < categoryList.size(); i++) {
            String category = categoryList.get(i);
//            String path =  WebDatabase.class.getResource("WebDatabase.class").toString();
//            path = path.substring(5, path.length() - "WebDatabase.class".length() - 1) + "/contentSummary/" + category + ".txt";
            String path = System.getProperty("user.dir") + "/contentSummary/" + category + ".txt";
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            ArrayList<String> list = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                String[] sArray = line.trim().split("\\s+");
                String query = new String();
                for (int j = 0; j < sArray.length; j++) {
                    if (j > 0) query += (sArray[j] + "%20");
                }
                query = query.substring(0, query.length() - 3);
                list.add(query);
            }
            rl.addAll(list);
            if (category.equals("Root")) cq.put(category, rl);
            else cq.put(category, list);
        }

        System.out.println("Extracting topic content Summary...");
        Iterator iterator = cq.entrySet().iterator();

        //iterate the TreeMap cq
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            String category = (String) pairs.getKey();
            ArrayList<String> queryList = (ArrayList<String>) pairs.getValue();
            System.out.println("Creating content summary for " + category);
            String outPath = System.getProperty("user.dir") + "/" + category + "-" + site + ".txt";
            FileWriter fw = new FileWriter(outPath);
            HashSet<String> set = new HashSet<String>();
            int count = 1;
            int n = queryList.size();
            TreeMap<String, Integer> map = new TreeMap<String, Integer>();
            for (int i = 0; i < queryList.size(); i++) {
                System.out.println(count++ + "/" + n);
                String query = queryList.get(i);
                String binUrl = prefix + query + suffix;
                JSONArray array = pageInfo(binUrl);
                for (int k = 0; k < array.length(); k++) {
                    String url = array.getJSONObject(k).getString("Url");
                    if (!set.contains(url)) {
                        set.add(url);
                        System.out.println("Getting pages: " + url);
                        System.out.println();
                        getWordsLynx lynx = new getWordsLynx();
                        Set<String> d = lynx.runLynx(url);//get words in each document
                        Iterator<String> iter = d.iterator();
                        //iterate the words set and add them to the map
                        while (iter.hasNext()) {
                            String word = iter.next();
                            if (!map.containsKey(word)) map.put(word, 1);
                            else map.put(word, map.get(word) + 1);
                        }
                    }
                }
            }
            Iterator iter = map.entrySet().iterator();
            //write results to a file in the form of "<word>#<frequency in the document sample>"
            while (iter.hasNext()) {
                Map.Entry p = (Map.Entry) iter.next();
                fw.write(p.getKey() + "#" + p.getValue() + "\n");
            }
            fw.close();
        }
    }

    //return true if element in the childList is also in the tmp
    private boolean isInList(ArrayList<String> tmp, ArrayList<String> childList) {
        for (int i = 0; i < childList.size(); i++) {
            if (tmp.contains(childList.get(i)))
                return true;
        }
        return false;
    }

    //parse bingUrl and return JSONObject
    private JSONObject parse(String bingUrl) throws IOException {
        URL url = new URL(bingUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

        InputStream inputStream = (InputStream) urlConnection.getContent();
        byte[] contentRaw = new byte[urlConnection.getContentLength()];
        inputStream.read(contentRaw);
        String content = new String(contentRaw);
        JSONObject obj = new JSONObject(content);
        return obj.getJSONObject("d").getJSONArray("results").getJSONObject(0);
    }

    //return the number of matches given bingUrl
    private int getMatches(String bingUrl) throws IOException {
        return Integer.valueOf(parse(bingUrl).getString("WebTotal"));
    }

    //return JSONArray given bingUrl
    private JSONArray pageInfo(String bingUrl) throws IOException{
        return parse(bingUrl).getJSONArray("Web");
    }

    public static void main(String[] args) throws IOException{
        if (args.length != 4) {
            System.out.println("There should be 4 parameters: <BING_ACCOUNT_KEY> <t_es> <t_ec> <host>");
            return;
        }
        String accountKey = args[0];
        double t_es = Double.valueOf(args[1]);
        double t_ec = Double.valueOf(args[2]);
        if (t_ec < 1 || t_es < 0 || t_es > 1) {
            System.out.println("specificity threshold should be 0 <= t_es <= 1, coverage threshold should be t_ec >= 1");
            return;
        }
        String host = args[3];
        WebDatabase w = new WebDatabase(accountKey, t_es, t_ec, host);
        ArrayList<String> classification = w.classification();
        w.summaryConstruction(classification);
    }
}