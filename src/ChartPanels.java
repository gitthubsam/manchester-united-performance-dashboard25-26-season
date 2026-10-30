import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

/** Self-contained visualisations of real match data. */
class ResultsChart extends JPanel{
        List<Match> matches=Collections.emptyList();
        ResultsChart(){setOpaque(false);setPreferredSize(new Dimension(420,208));}
        void setMatches(List<Match> l){matches=new ArrayList<>(l);repaint();}
        @Override protected void paintComponent(Graphics g){
            Graphics2D gg=(Graphics2D)g.create();
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            if(matches.isEmpty()){ChartPanels.empty(gg,w,h);gg.dispose();return;}
            SortedMap<YearMonth,int[]> months=new TreeMap<>();
            for(Match m:matches){int[] counts=months.computeIfAbsent(YearMonth.from(m.date),k->new int[3]);counts[m.result()=='W'?0:m.result()=='D'?1:2]++;}
            List<Map.Entry<YearMonth,int[]>> entries=new ArrayList<>(months.entrySet());
            if(entries.size()>8)entries=entries.subList(entries.size()-8,entries.size());
            int max=1;for(var entry:entries){int[] a=entry.getValue();max=Math.max(max,a[0]+a[1]+a[2]);}
            int left=35,right=w-10,top=27,bottom=h-37;
            gg.setFont(DashboardTheme.uiFont(Font.PLAIN,10));
            for(int i=0;i<=3;i++){
                int y=bottom-(bottom-top)*i/3;
                gg.setColor(DashboardTheme.BORDER);gg.drawLine(left,y,right,y);
                gg.setColor(DashboardTheme.MUTED);gg.drawString(""+(int)Math.round(max*i/3.0),4,y+4);
            }
            int count=entries.size(),slot=Math.max(1,(right-left)/count);
            for(int i=0;i<count;i++){
                int[] a=entries.get(i).getValue();int total=a[0]+a[1]+a[2];
                int x=left+slot*i+slot/2;int barWidth=Math.min(33,Math.max(12,slot-12));
                int y=bottom;Color[] colors={DashboardTheme.GREEN,DashboardTheme.AMBER,DashboardTheme.RED};
                for(int k=0;k<3;k++){
                    int height=(int)Math.round((bottom-top)*(a[k]*1.0/max));
                    if(height==0&&a[k]>0)height=1;
                    y-=height;gg.setColor(colors[k]);gg.fillRect(x-barWidth/2,y,barWidth,height);
                }
                gg.setColor(DashboardTheme.MUTED);
                String name=entries.get(i).getKey().getMonth().getDisplayName(java.time.format.TextStyle.SHORT,Locale.UK)+" "+
                    (entries.get(i).getKey().getYear()%100);
                gg.drawString(name,x-16,h-18);
            }
            int legendX=left;
            String[] labels={"Wins","Draws","Losses"};Color[] colors={DashboardTheme.GREEN,DashboardTheme.AMBER,DashboardTheme.RED};
            for(int i=0;i<3;i++){gg.setColor(colors[i]);gg.fillRect(legendX,2,8,8);gg.setColor(DashboardTheme.MUTED);gg.drawString(labels[i],legendX+12,10);legendX+=69;}
            gg.dispose();
        }
    }
class GoalsChart extends JPanel {
        List<Match> matches=Collections.emptyList();
        GoalsChart(){setOpaque(false);setPreferredSize(new Dimension(420,208));}
        void setMatches(List<Match> l){matches=new ArrayList<>(l);repaint();}
        @Override protected void paintComponent(Graphics g){
            Graphics2D gg=(Graphics2D)g.create();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();if(matches.isEmpty()){ChartPanels.empty(gg,w,h);gg.dispose();return;}
            List<Match> last=matches.size()>12?matches.subList(matches.size()-12,matches.size()):matches;
            int max=3;for(Match m:last)max=Math.max(max,Math.max(m.goalsFor,m.goalsAgainst));
            int left=31,right=w-21,top=31,bottom=h-37;
            gg.setFont(DashboardTheme.uiFont(Font.PLAIN,10));
            for(int k=0;k<=max;k++){
                int y=bottom-(bottom-top)*k/max;gg.setColor(DashboardTheme.BORDER);gg.drawLine(left,y,right,y);
                gg.setColor(DashboardTheme.MUTED);gg.drawString(""+k,7,y+4);
            }
            Color[] colors={DashboardTheme.GREEN,DashboardTheme.RED};
            for(int type=0;type<2;type++){
                gg.setColor(colors[type]);gg.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                Path2D path=new Path2D.Double();
                for(int i=0;i<last.size();i++){
                    Match m=last.get(i);int goals=type==0?m.goalsFor:m.goalsAgainst;
                    double x=last.size()==1?(left+right)/2.0:left+(right-left)*i/(double)(last.size()-1);
                    double y=bottom-(bottom-top)*goals/(double)max;
                    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                }
                gg.draw(path);
                for(int i=0;i<last.size();i++){
                    Match m=last.get(i);int goals=type==0?m.goalsFor:m.goalsAgainst;
                    int x=(int)Math.round(last.size()==1?(left+right)/2.0:left+(right-left)*i/(double)(last.size()-1));
                    int y=bottom-(bottom-top)*goals/max;
                    gg.fillOval(x-4,y-4,8,8);
                }
            }
            gg.setStroke(new BasicStroke(1));gg.setColor(DashboardTheme.MUTED);
            for(int i=0;i<last.size();i++){
                if(last.size()>8&&i%2==1)continue;
                int x=(int)Math.round(last.size()==1?(left+right)/2.0:left+(right-left)*i/(double)(last.size()-1));
                gg.drawString(last.get(i).date.format(DateTimeFormatter.ofPattern("dd MMM",Locale.UK)),x-13,h-18);
            }
            gg.setColor(DashboardTheme.GREEN);gg.fillRect(left,2,8,8);gg.setColor(DashboardTheme.MUTED);gg.drawString("Scored",left+12,10);
            gg.setColor(DashboardTheme.RED);gg.fillRect(left+88,2,8,8);gg.setColor(DashboardTheme.MUTED);gg.drawString("Conceded",left+100,10);
            gg.dispose();
        }
    }
final class ChartPanels {
    private ChartPanels() {}
    static void empty(Graphics2D g,int w,int h){
        g.setColor(DashboardTheme.MUTED);g.setFont(DashboardTheme.uiFont(Font.PLAIN,13));
        String message="No fixtures match these filters";
        FontMetrics fm=g.getFontMetrics();g.drawString(message,(w-fm.stringWidth(message))/2,h/2);
    }

}
