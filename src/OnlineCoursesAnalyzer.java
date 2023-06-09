import com.sun.source.tree.Tree;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1

    /**
     * @return: PtcpCountByInst
     */
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> ret = new LinkedHashMap<>();
        ret = courses.stream()
                .sorted((c1, c2) -> c1.institution.compareTo(c2.institution))
                .collect(Collectors.groupingBy(c -> c.institution, LinkedHashMap::new, Collectors.summingInt(c -> c.participants)));
        return ret;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> ret = new LinkedHashMap<>();
        Map<String, Integer> mapUnSorted = new HashMap<>();
        mapUnSorted = courses.stream()
                .collect(Collectors.groupingBy(c -> c.institution + "-" + c.subject, Collectors.summingInt(c -> c.participants)));
        ret = mapUnSorted
                .entrySet()
                .stream()
                .sorted((a, b) -> {
                    if (a.getValue() != b.getValue()) {
                        return -Integer.compare(a.getValue(), b.getValue());
                    } else {
                        return a.getKey().compareTo(b.getKey());
                    }
                })
                .collect(Collectors
                    .toMap(a -> a.getKey(), a -> a.getValue(), (a1, a2) -> a1, LinkedHashMap::new));
        return ret;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> ret = new HashMap<>();
        Map<String, Set<String>> independentTmp = new HashMap<>();
        Map<String, Set<String>> codependentTmp = new HashMap<>();
        Set<String> instructors = new TreeSet<>();
        instructors = courses
                .stream()
                .flatMap(c -> Arrays
                    .stream(c.instructors.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1)))
                .map(i -> {
                    if (i.charAt(0) == ' ') return i.substring(1);
                    return i;
                })
                .collect(Collectors.toSet());
        independentTmp = courses
                .stream()
                .filter(c -> c.instructors.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1).length == 1)
                .flatMap(c -> Arrays.stream(c.instructors.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1))
                        .map(a -> {
                            Course nc = c;
                            if (a.charAt(0) != ' ')nc.instructors = a;
                            else nc.instructors = a.substring(1);
                            return nc;
                        }))
                .collect(Collectors.groupingBy(c -> c.instructors, Collectors.mapping(c -> c.title, Collectors.toSet())));
        codependentTmp = courses
                .stream()
                .filter(c -> c.instructors.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1).length != 1)
                .flatMap(c -> Arrays.stream(c.instructors.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1))
                        .map(a -> {
                            Course nc = c;
                            if (a.charAt(0) != ' ') nc.instructors = a;
                            else nc.instructors = a.substring(1);
                            return nc;
                        }))
                .collect(Collectors.groupingBy(c -> c.instructors, Collectors.mapping(c -> c.title, Collectors.toSet())));
        for (String instructor : instructors) {
            List<List<String>> lists = new ArrayList<List<String>>(2);
            lists.add(new ArrayList<>());
            lists.add(new ArrayList<>());
            if (independentTmp.containsKey(instructor)) {
                Set<String> s = independentTmp.get(instructor);
                lists.set(0, s.stream().sorted((a, b) -> a.compareTo(b)).toList());
            }
            if (codependentTmp.containsKey(instructor)) {
                Set<String> s = codependentTmp.get(instructor);
                lists.set(1, s.stream().sorted((a, b) -> a.compareTo(b)).toList());
            }
            ret.put(instructor, lists);
        }
        return ret;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> ret = new ArrayList<>();
        Comparator<Course> comparator = (a, b) -> {
            if (by.equals("hours")) {
                if (a.totalHours == b.totalHours) {
                    return a.title.compareTo(b.title);
                }
                return -Double.compare(a.totalHours, b.totalHours);
            } else { // by participants
                if (a.participants == b.participants) {
                    return a.title.compareTo(b.title);
                }
                return -Integer.compare(a.participants, b.participants);
            }
        };
        ret = courses
                .stream()
                .sorted(comparator)
                .map(c -> c.title)
                .distinct()
                .limit(topK)
                .toList();
        return ret;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        List<String> ret = new ArrayList<>();
        ret = courses
                .stream()
                .filter(c -> c.percentAudited >= percentAudited && c.totalHours <= totalCourseHours)
                .filter(c -> c.subject.toLowerCase().contains(courseSubject.toLowerCase()))
                .map(c -> c.title)
                .distinct()
                .sorted((t1, t2) -> t1.compareTo(t2))
                .toList();
        return ret;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        List<String> ret = new ArrayList<>();
        Map<String, Optional<Course>> numberToLatestCourse = new HashMap<>();
        Comparator<Map.Entry<String, List<Course>>> comparator = (c1, c2) -> {
            double medianAgec1 = c1.getValue().stream().mapToDouble(c -> c.medianAge).average().getAsDouble();
            double medianAgec2 = c2.getValue().stream().mapToDouble(c -> c.medianAge).average().getAsDouble();
            double percentMalec1 = c1.getValue().stream().mapToDouble(c -> c.percentMale).average().getAsDouble();
            double percentMalec2 = c2.getValue().stream().mapToDouble(c -> c.percentMale).average().getAsDouble();
            double percentDegreec1 = c1.getValue().stream().mapToDouble(c -> c.percentDegree).average().getAsDouble();
            double percentDegreec2 = c2.getValue().stream().mapToDouble(c -> c.percentDegree).average().getAsDouble();
            double v1 = (medianAgec1 - age) * (medianAgec1 - age)
                    + (100.0 * gender - percentMalec1) * (100.0 * gender - percentMalec1)
                    + (100.0 * isBachelorOrHigher - percentDegreec1) * (100.0 * isBachelorOrHigher - percentDegreec1);
            double v2 = (medianAgec2 - age) * (medianAgec2 - age)
                    + (100.0 * gender - percentMalec2) * (100.0 * gender - percentMalec2)
                    + (100.0 * isBachelorOrHigher - percentDegreec2) *  (100.0 * isBachelorOrHigher - percentDegreec2);
            if (v1 != v2) {
                return Double.compare(v1, v2);
            }
            else {
                Course n1 = c1.getValue().stream().max((a, b) -> a.launchDate.compareTo(b.launchDate)).get();
                Course n2 = c2.getValue().stream().max((a, b) -> a.launchDate.compareTo(b.launchDate)).get();
                return n1.title.compareTo(n2.title);
            }
        };
        numberToLatestCourse = courses
                .stream()
                .collect(Collectors.groupingBy(c -> c.number,
                    Collectors.maxBy((a, b) -> a.launchDate.compareTo(b.launchDate))));
        Map<String, Optional<Course>> finalNumberToLatestCourse = numberToLatestCourse;
        ret = courses
                .stream()
                .collect(Collectors.groupingBy(c -> c.number, Collectors.toList()))
                .entrySet()
                .stream()
                .sorted(comparator)
                .map(c -> finalNumberToLatestCourse.get(c.getKey()).get().title)
                .distinct()
                .limit(10)
                .toList();
        return ret;
    }

}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) {
            title = title.substring(1);
        }
        if (title.endsWith("\"")) {
            title = title.substring(0, title.length() - 1);
        }
        this.title = title;
        if (instructors.startsWith("\"")) {
            instructors = instructors.substring(1);
        }
        if (instructors.endsWith("\"")) {
            instructors = instructors.substring(0, instructors.length() - 1);
        }
        this.instructors = instructors;
        if (subject.startsWith("\"")) {
            subject = subject.substring(1);
        }
        if (subject.endsWith("\"")) {
            subject = subject.substring(0, subject.length() - 1);
        }
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
}