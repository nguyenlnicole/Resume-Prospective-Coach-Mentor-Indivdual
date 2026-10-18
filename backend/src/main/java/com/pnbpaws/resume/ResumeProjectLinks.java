package com.pnbpaws.resume;
import java.util.*;
final class ResumeProjectLinks {
  private static String key(String heading) {
    return heading.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)","$1").split("\\|",2)[0]
        .split("\\s+[—–]\\s+",2)[0].replaceFirst("^[•*\\-]\\s+","")
        .replaceFirst("(?i)\\s+Web\\s*(?:Page|Application)$","").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");
  }
  static String preserve(String original, String rewritten) {
    Map<String,List<String>> headings=new HashMap<>();
    for(String block:original.split("\\n\\s*\\n")) {
      String heading=block.lines().findFirst().orElse("");String name=key(heading);
      if(!name.isBlank())headings.computeIfAbsent(name,ignored->new ArrayList<>()).add(heading);
    }
    String[] projects=rewritten.split("\\n\\s*\\n");
    for(int index=0;index<projects.length;index++){
      String heading=projects[index].lines().findFirst().orElse("");var supplied=headings.get(key(heading));
      if(supplied!=null && supplied.size()==1)projects[index]=supplied.getFirst()+projects[index].substring(heading.length());
    }
    return String.join("\n\n",projects);
  }
}
