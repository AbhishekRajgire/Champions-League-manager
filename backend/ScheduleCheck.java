import java.util.*;

/**
 * Standalone verification of the fixture generator + new matchday scheduler.
 * Mirrors FixtureGenerationService on synthetic data, then asserts all rules
 * and that matchdays are exactly GAMES_PER_TEAM, each a perfect matching.
 *
 * Run:  java ScheduleCheck.java
 */
public class ScheduleCheck {
    static final int POTS = 4;
    static final int OPPONENTS_PER_POT = 2;
    static final int MAX_PER_COUNTRY = 2;

    public static void main(String[] args) {
        int perPot = 9;                 // 36 teams, like the real CL
        int n = perPot * POTS;
        int[] potOf = new int[n];
        String[] countryOf = new String[n];
        // Spread teams across 8 countries so the country constraints actually bite.
        String[] countries = {"esp","eng","ger","ita","fra","por","ned","bel"};
        for (int i = 0; i < n; i++) {
            potOf[i] = i / perPot;
            countryOf[i] = countries[i % countries.length];
        }

        Random random = new Random(12345);
        Set<Long> edges = null;
        for (int attempt = 1; attempt <= 20000 && edges == null; attempt++) {
            Builder b = new Builder(n, potOf, countryOf, random);
            if (b.build()) edges = b.edges;
        }
        if (edges == null) { System.out.println("FAIL: could not build graph"); return; }

        List<int[]> directed = orient(edges, n, potOf, random);
        int gamesPerTeam = OPPONENTS_PER_POT * POTS;
        int matchdays = assignMatchdays(directed, n, gamesPerTeam, random);

        verify(directed, n, potOf, countryOf, gamesPerTeam, matchdays);
    }

    // ---- verification ----
    static void verify(List<int[]> d, int n, int[] potOf, String[] countryOf, int gpt, int matchdays) {
        boolean ok = true;
        int[] games = new int[n], home = new int[n], away = new int[n];
        int[][] vsPot = new int[n][POTS];
        Map<Integer,Map<String,Integer>> oppCountry = new HashMap<>();
        for (int i=0;i<n;i++) oppCountry.put(i,new HashMap<>());

        for (int[] m : d) {
            int h=m[0],a=m[1];
            games[h]++; games[a]++; home[h]++; away[a]++;
            vsPot[h][potOf[a]]++; vsPot[a][potOf[h]]++;
            oppCountry.get(h).merge(countryOf[a],1,Integer::sum);
            oppCountry.get(a).merge(countryOf[h],1,Integer::sum);
            if (countryOf[h].equals(countryOf[a])) { System.out.println("FAIL same-country: "+h+" vs "+a); ok=false; }
        }
        for (int i=0;i<n;i++) {
            if (games[i]!=gpt){System.out.println("FAIL games team "+i+"="+games[i]); ok=false;}
            if (home[i]!=gpt/2||away[i]!=gpt/2){System.out.println("FAIL home/away team "+i+" h="+home[i]+" a="+away[i]); ok=false;}
            for (int p=0;p<POTS;p++) if (vsPot[i][p]!=OPPONENTS_PER_POT){System.out.println("FAIL team "+i+" vs pot"+p+"="+vsPot[i][p]); ok=false;}
            for (var e: oppCountry.get(i).entrySet()) if (e.getValue()>MAX_PER_COUNTRY){System.out.println("FAIL team "+i+" faces "+e.getValue()+" "+e.getKey()); ok=false;}
        }
        // matchday balance: each team once per matchday, balanced counts
        Map<Integer,Integer> perMd = new TreeMap<>();
        Map<Integer,Set<Integer>> teamsPerMd = new HashMap<>();
        for (int[] m : d) {
            int md=m[2];
            perMd.merge(md,1,Integer::sum);
            var set = teamsPerMd.computeIfAbsent(md,k->new HashSet<>());
            if(!set.add(m[0])||!set.add(m[1])){System.out.println("FAIL team twice on matchday "+md); ok=false;}
        }
        System.out.println("Teams="+n+"  matches="+d.size()+"  matchdays="+matchdays);
        System.out.println("matches per matchday: "+perMd);
        if (matchdays!=gpt){System.out.println("FAIL expected "+gpt+" matchdays, got "+matchdays); ok=false;}
        for (var e: perMd.entrySet()) if (e.getValue()!=n/2){System.out.println("FAIL matchday "+e.getKey()+" has "+e.getValue()+" (expected "+n/2+")"); ok=false;}

        System.out.println(ok ? "\nALL CHECKS PASSED ✓" : "\nSOME CHECKS FAILED ✗");
    }

    // ================= copied logic =================
    static final class Builder {
        final int n; final int[] potOf; final String[] countryOf; final Random random;
        final int[][] demand; final List<Set<Integer>> opponents; final List<Map<String,Integer>> countryCount;
        final Set<Long> edges=new HashSet<>(); int steps=0; final int stepBudget;
        Builder(int n,int[] potOf,String[] countryOf,Random random){this.n=n;this.potOf=potOf;this.countryOf=countryOf;this.random=random;
            demand=new int[n][POTS]; opponents=new ArrayList<>(n); countryCount=new ArrayList<>(n);
            for(int i=0;i<n;i++){Arrays.fill(demand[i],OPPONENTS_PER_POT);opponents.add(new HashSet<>());countryCount.add(new HashMap<>());}
            stepBudget=200*n;}
        boolean build(){return solve();}
        boolean solve(){ if(++steps>stepBudget) return false;
            int bestTeam=-1,bestPot=-1; List<Integer> best=null;
            for(int i=0;i<n;i++) for(int p=0;p<POTS;p++){ if(demand[i][p]<=0) continue;
                List<Integer> c=candidatesFor(i,p); if(c.isEmpty()) return false;
                if(best==null||c.size()<best.size()){best=c;bestTeam=i;bestPot=p; if(c.size()==1){p=POTS;i=n;}}}
            if(best==null) return true;
            Collections.shuffle(best,random);
            for(int j:best){addEdge(bestTeam,bestPot,j); if(solve()) return true; removeEdge(bestTeam,bestPot,j);} return false;}
        List<Integer> candidatesFor(int i,int p){ List<Integer> r=new ArrayList<>(); int pi=potOf[i]; Set<Integer> opp=opponents.get(i); Map<String,Integer> ccI=countryCount.get(i);
            for(int j=0;j<n;j++){ if(j==i)continue; if(potOf[j]!=p)continue; if(demand[j][pi]<=0)continue; if(opp.contains(j))continue;
                if(countryOf[i].equals(countryOf[j]))continue; if(ccI.getOrDefault(countryOf[j],0)>=MAX_PER_COUNTRY)continue;
                if(countryCount.get(j).getOrDefault(countryOf[i],0)>=MAX_PER_COUNTRY)continue; r.add(j);} return r;}
        void addEdge(int i,int p,int j){int pi=potOf[i];demand[i][p]--;demand[j][pi]--;opponents.get(i).add(j);opponents.get(j).add(i);
            bump(countryCount.get(i),countryOf[j],1);bump(countryCount.get(j),countryOf[i],1);edges.add(key(i,j));}
        void removeEdge(int i,int p,int j){int pi=potOf[i];demand[i][p]++;demand[j][pi]++;opponents.get(i).remove(j);opponents.get(j).remove(i);
            bump(countryCount.get(i),countryOf[j],-1);bump(countryCount.get(j),countryOf[i],-1);edges.remove(key(i,j));}
        void bump(Map<String,Integer> m,String c,int d){int v=m.getOrDefault(c,0)+d; if(v<=0)m.remove(c); else m.put(c,v);}
        long key(int a,int b){int lo=Math.min(a,b),hi=Math.max(a,b);return (long)lo*n+hi;}
    }

    static List<int[]> orient(Set<Long> edges,int n,int[] potOf,Random random){
        Map<Integer,List<int[]>> groups=new HashMap<>();
        for(long e:edges){int a=(int)(e/n),b=(int)(e%n);int pa=potOf[a],pb=potOf[b];int g=Math.min(pa,pb)*POTS+Math.max(pa,pb);
            groups.computeIfAbsent(g,k->new ArrayList<>()).add(new int[]{a,b});}
        List<int[]> directed=new ArrayList<>();
        for(List<int[]> grp:groups.values()) directed.addAll(orientGroup(grp,random));
        return directed;}
    static List<int[]> orientGroup(List<int[]> ge,Random random){int m=ge.size();boolean[] used=new boolean[m];
        Map<Integer,List<int[]>> adj=new HashMap<>();
        for(int idx=0;idx<m;idx++){int a=ge.get(idx)[0],b=ge.get(idx)[1];adj.computeIfAbsent(a,k->new ArrayList<>()).add(new int[]{b,idx});adj.computeIfAbsent(b,k->new ArrayList<>()).add(new int[]{a,idx});}
        List<int[]> directed=new ArrayList<>(m);
        for(int s=0;s<m;s++){ if(used[s])continue; int cur=ge.get(s)[0];
            while(true){int ce=-1,next=-1; for(int[] na:adj.get(cur)) if(!used[na[1]]){next=na[0];ce=na[1];break;} if(ce==-1)break; used[ce]=true; directed.add(new int[]{cur,next,0}); cur=next;}}
        return directed;}

    static int assignMatchdays(List<int[]> directed,int n,int rounds,Random random){
        for(int attempt=0;attempt<400;attempt++){int[] c=tryColourExact(directed,n,rounds,random);
            if(c!=null){for(int i=0;i<directed.size();i++) directed.get(i)[2]=c[i]+1; return rounds;}}
        return assignMatchdaysGreedy(directed,n,random);}
    static int[] tryColourExact(List<int[]> directed,int n,int colours,Random random){int m=directed.size();int[] order=new int[m];
        for(int i=0;i<m;i++)order[i]=i; for(int i=m-1;i>0;i--){int j=random.nextInt(i+1);int t=order[i];order[i]=order[j];order[j]=t;}
        int[] colour=new int[m];Arrays.fill(colour,-1);int[] used=new int[n];int[] budget={100*m};
        return colourEdges(0,order,directed,colour,used,colours,budget,random)?colour:null;}
    static boolean colourEdges(int pos,int[] order,List<int[]> directed,int[] colour,int[] used,int colours,int[] budget,Random random){
        if(pos==order.length)return true; if(--budget[0]<0)return false; int e=order[pos];int u=directed.get(e)[0],v=directed.get(e)[1];int forb=used[u]|used[v];
        List<Integer> avail=new ArrayList<>(); for(int c=0;c<colours;c++) if((forb&(1<<c))==0) avail.add(c); Collections.shuffle(avail,random);
        for(int c:avail){int bit=1<<c;colour[e]=c;used[u]|=bit;used[v]|=bit; if(colourEdges(pos+1,order,directed,colour,used,colours,budget,random))return true; colour[e]=-1;used[u]&=~bit;used[v]&=~bit;} return false;}
    static int assignMatchdaysGreedy(List<int[]> directed,int n,Random random){Collections.shuffle(directed,random);List<boolean[]> busy=new ArrayList<>();int max=0;
        for(int[] m:directed){int h=m[0],a=m[1];int md=0;while(true){if(md==busy.size())busy.add(new boolean[n]);boolean[] day=busy.get(md);if(!day[h]&&!day[a]){day[h]=true;day[a]=true;break;}md++;}m[2]=md+1;max=Math.max(max,md+1);}return max;}
}
