import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        OnlineCoursesAnalyzer onlineCoursesAnalyzer = new OnlineCoursesAnalyzer("resources/local.csv");
        Map<String, List<List<String>>> map = onlineCoursesAnalyzer.getCourseListOfInstructor();
        for(String key : map.keySet()) {
            System.out.println(key + " == " + map.get(key));
        }
    }
}
