package com.pnbpaws.resume;
import java.util.*;
import java.util.regex.*;
final class ExperienceOrder {
  private static final Pattern DATE=Pattern.compile("(?i)(?:(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)\\.?\\s+|(\\d{1,2})/)?((?:19|20)\\d{2})");
  private static final List<String> MONTHS=List.of("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec");
  private static boolean bullet(String line){return line.matches("^\\s*[•*\\-]\\s+.*");}
  private static boolean dated(String line){return !bullet(line) && DATE.matcher(line).find();}
  private record Entry(String text,boolean current,int start,int end) {}
  private static Entry entry(List<String> lines){
    String heading=String.join(" ",lines.stream().filter(line->!bullet(line)).toList());
    Matcher dates=DATE.matcher(heading);int start=-1,end=-1;
    while(dates.find()){
      int month=dates.group(1)!=null?MONTHS.indexOf(dates.group(1).substring(0,3).toLowerCase(Locale.ROOT)):dates.group(2)!=null?Integer.parseInt(dates.group(2))-1:0;
      end=Integer.parseInt(dates.group(3))*12+month;if(start<0)start=end;
    }
    return new Entry(String.join("\n",lines),Pattern.compile("(?i)\\b(present|current)\\b").matcher(heading).find(),start,end);
  }
  static String sort(String text){
    List<Entry> entries=new ArrayList<>();List<String> lines=new ArrayList<>();String employer="";
    for(String raw:text.split("\\R")){
      String line=raw.strip();
      if(line.isBlank()){if(!lines.isEmpty())entries.add(entry(lines));lines=new ArrayList<>();employer="";continue;}
      if(dated(line) && lines.stream().anyMatch(ExperienceOrder::dated)){
        entries.add(entry(lines));lines=new ArrayList<>();if(!employer.isBlank())lines.add(employer);
      }
      if(!bullet(line) && !dated(line) && lines.isEmpty())employer=line;
      lines.add(line);
    }
    if(!lines.isEmpty())entries.add(entry(lines));
    entries.sort(Comparator.comparing(Entry::current).reversed().thenComparing(Comparator.comparingInt((Entry e)->e.current()?e.start():e.end()).reversed()).thenComparing(Comparator.comparingInt(Entry::start).reversed()));
    return String.join("\n\n",entries.stream().map(Entry::text).toList());
  }
}
