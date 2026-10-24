package com.vocr.replay;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Isolated untrusted-file boundary. Never feeds rawRecords to fromRecords or loads the live manager. */
public final class ReplayFileExchange {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final long SAFE_INTEGER = 9007199254740991L;
    private static final List<String> CONFIGS = List.of("jianyi","tongchang","yaoohu","kuangxin","beide","maoyou","daxing");
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32)
                    .maxStringLength(65536).maxNumberLength(128).build()).build())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final Pattern FORBIDDEN_KEY = Pattern.compile("(?i)^(?:__proto__|prototype|constructor|sessionid|sessiontoken|accesstoken|authorization|password|cookie|url|href|src|path)$");
    private static final Pattern REFERENCE = Pattern.compile("(?i)^(?:[a-z][a-z0-9+.-]*:|[/\\\\]|\\.\\.[/\\\\])");
    private ReplayFileExchange() {}
    public static final class Problem extends IOException {
        private final String code;
        public Problem(String code) { super(code); this.code=code; }
        public String code() { return code; }
    }
    public static final class Candidate {
        private final int fileSlot, sourceVersion;
        private final ObjectNode data;
        private Candidate(int slot,int version,ObjectNode normalized) {
            fileSlot=slot;sourceVersion=version;data=normalized.deepCopy();
        }
        public int fileSlot() { return fileSlot; }
        public int sourceVersion() { return sourceVersion; }
        public boolean readOnly() { return data.path("endResult").asInt()==0; }
        public JsonNode data() { return data.deepCopy(); }
        public ReplaySave replay() throws IOException { return MAPPER.treeToValue(data,ReplaySave.class); }
    }
    private static void require(boolean condition) throws Problem { if(!condition)throw new Problem("INVALID_REPLAY"); }
    private static void object(JsonNode n,String... fields) throws Problem {
        require(n!=null && n.isObject());for(String f:fields)require(n.has(f));
    }
    private static long integer(JsonNode n,long min,long max) throws Problem {
        require(n!=null && n.isNumber());
        try {long v=n.decimalValue().longValueExact();require(v>=min && v<=max && Math.abs(v)<=SAFE_INTEGER);return v;}
        catch(ArithmeticException e){throw new Problem("INVALID_REPLAY");}
    }
    private static int number(JsonNode n,String key,int min,int max) throws Problem {return (int)integer(n.get(key),min,max);}
    private static String text(JsonNode n) throws Problem {require(n!=null && n.isTextual());return n.textValue();}
    private static void array(JsonNode n,int min,int max) throws Problem {require(n!=null && n.isArray() && n.size()>=min && n.size()<=max);}
    private static void safe(JsonNode n,String key) throws Problem {
        if(n.isNumber()) {
            if(n.isIntegralNumber() || n.decimalValue().stripTrailingZeros().scale()<=0)integer(n,-SAFE_INTEGER,SAFE_INTEGER);
            else require(Double.isFinite(n.doubleValue()));
        }
        if(n.isTextual() && !key.equals("rawRecords"))require(n.textValue().getBytes(StandardCharsets.UTF_8).length<=65536 && !REFERENCE.matcher(n.textValue()).find());
        if(n.isArray())for(JsonNode v:n)safe(v,key);
        if(n.isObject()) {
            var fields=n.fields();
            while(fields.hasNext()) {var entry=fields.next();require(!FORBIDDEN_KEY.matcher(entry.getKey()).matches());safe(entry.getValue(),entry.getKey());}
        }
    }
    private static void defaultField(ObjectNode n,String key,JsonNode value) {if(!n.has(key))n.set(key,value);}
    private static ObjectNode normalize(JsonNode source,int slot) throws IOException {
        object(source,"version","slotIndex");
        long version=integer(source.get("version"),0,SAFE_INTEGER);
        if(version>3)throw new Problem("UNSUPPORTED_VERSION");
        require(number(source,"slotIndex",0,8)==slot);
        var r=(ObjectNode)source.deepCopy();r.put("version",3);r.put("slotIndex",0);
        for(String key:List.of("peiyiVillageCount","totalVillageCount")) {
            if(version<=1 && (!r.has(key) || (r.get(key).isNumber() && integer(r.get(key),-SAFE_INTEGER,SAFE_INTEGER)<=0)))r.put(key,-1);
            else defaultField(r,key,IntNode.valueOf(0));
        }
        array(r.get("daySnapshots"),1,50);
        for(JsonNode day:r.get("daySnapshots")) {
            object(day);var d=(ObjectNode)day;
            defaultField(d,"dailyVotingRule",IntNode.valueOf(-1));
            for(String key:List.of("voteRounds","greyTargetCharNums","designatedTargetCharNums"))defaultField(d,key,MAPPER.createArrayNode());
            array(d.get("players"),1,44);
            for(JsonNode player:d.get("players")){object(player);defaultField((ObjectNode)player,"skillTargets",MAPPER.createArrayNode());}
        }
        try {validate(r);}catch(Problem problem){if(version<3 && problem.code().equals("INVALID_REPLAY"))throw new Problem("LEGACY_COMPATIBILITY_DIAGNOSTIC");throw problem;}return r;
    }
    private static void validate(ObjectNode r) throws IOException {
        safe(r,"");
        object(r,"saveTime","peiyiName","peiyiOrdinal","peiyiVillageCount","totalVillageCount","totalDays","endResult","totalEvents","daySnapshots","rawRecords");
        require(text(r.get("saveTime")).matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        int ordinal=number(r,"peiyiOrdinal",1,7);require(CONFIGS.get(ordinal-1).equals(text(r.get("peiyiName"))));
        for(String key:List.of("peiyiVillageCount","totalVillageCount"))integer(r.get(key),-1,SAFE_INTEGER);
        int days=number(r,"totalDays",1,50),result=number(r,"endResult",0,3);
        array(r.get("rawRecords"),1,20000);require(integer(r.get("totalEvents"),1,20000)==r.get("rawRecords").size());
        String end=null;
        for(JsonNode line:r.get("rawRecords")){String s=text(line);require(s.getBytes(StandardCharsets.UTF_8).length<=65536);if(s.startsWith("END|"))end=s;}
        int endDay=0;
        if(result==0 && end!=null) {
            var match=Pattern.compile("^END\\|result=UNFINISHED\\|day=(0|[1-9][0-9]*)$").matcher(end);
            require(match.matches());
            try{endDay=Integer.parseInt(match.group(1));}catch(NumberFormatException e){throw new Problem("INVALID_REPLAY");}
            require(endDay>=0 && endDay<=days);
        }
        else if(result!=0) {
            require(end!=null);var match=Pattern.compile("^END\\|result=(VILLAGE_WIN|WOLF_WIN|FOX_WIN)\\|day=([1-9][0-9]*)$").matcher(end);
            require(match.matches());require(List.of("VILLAGE_WIN","WOLF_WIN","FOX_WIN").get(result-1).equals(match.group(1)));
            try {endDay=Integer.parseInt(match.group(2));}catch(NumberFormatException e){throw new Problem("INVALID_REPLAY");}
            require(endDay>=1 && endDay<=days);
        }
        int last=0;Map<Integer,Integer> identities=null;
        for(JsonNode day:r.get("daySnapshots")) {
            object(day,"dayNumber","aliveCount","players","voteRounds","dailyVotingRule","greyTargetCharNums","designatedTargetCharNums");
            last=number(day,"dayNumber",last+1,days);
            int count=day.get("players").size();var ids=new TreeMap<Integer,Integer>();var chars=new HashSet<Integer>();
            for(JsonNode p:day.get("players")) {
                object(p,"playerIndex","characterNumber","actualRole","claimedRole","claimedRoleOrder","comingOutDay","skillTarget","skillTargets","deathDay","deathReason","nonHumanMarked");
                int index=number(p,"playerIndex",1,count),character=number(p,"characterNumber",1,44);
                require(ids.put(index,character)==null && chars.add(character));
                number(p,"actualRole",1,11);number(p,"claimedRole",0,11);number(p,"claimedRoleOrder",0,44);
                number(p,"comingOutDay",-1,50);number(p,"deathDay",0,50);number(p,"deathReason",0,7);
                require(p.get("nonHumanMarked").isBoolean());number(p,"skillTarget",0,count*2);
                array(p.get("skillTargets"),0,51);for(JsonNode target:p.get("skillTargets"))integer(target,0,count*2);
            }
            require(identities==null || identities.equals(ids));identities=ids;
            number(day,"aliveCount",0,count);number(day,"dailyVotingRule",-1,2);
            for(String key:List.of("greyTargetCharNums","designatedTargetCharNums")) {
                array(day.get(key),0,key.equals("greyTargetCharNums")?count:44);var set=new HashSet<Long>();
                for(JsonNode n:day.get(key)){long v=integer(n,1,key.equals("greyTargetCharNums")?count:44);require(set.add(v));if(key.equals("designatedTargetCharNums"))require(chars.contains((int)v));}
            }
            array(day.get("voteRounds"),0,100);int round=0;
            for(JsonNode vote:day.get("voteRounds")) {
                object(vote,"round","votes");round=number(vote,"round",round+1,100);array(vote.get("votes"),0,count);var voters=new HashSet<Long>();
                for(JsonNode pair:vote.get("votes")){array(pair,2,2);require(voters.add(integer(pair.get(0),1,count)));integer(pair.get(1),0,count);}
            }
        }
        require(days==Math.max(last,endDay));
        for(String key:List.of("peiyiDisplayName","resultDisplayName"))if(r.has(key))require(text(r.get(key)).length()<=256);
        if(MAPPER.writeValueAsBytes(r).length>MAX_BYTES)throw new Problem("REPLAY_TOO_LARGE");
    }
    public static List<Candidate> parse(byte[] bytes) throws IOException {
        if(bytes.length>MAX_BYTES)throw new Problem("REPLAY_TOO_LARGE");
        String text;
        try {text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException e){throw new Problem("INVALID_UTF8");}
        if(text.startsWith("\uFEFF"))text=text.substring(1);
        JsonNode root;
        try {
            try(var parser=MAPPER.createParser(text)) {
                int values=0;JsonToken token;
                while((token=parser.nextToken())!=null) {
                    if(token!=JsonToken.FIELD_NAME && token!=JsonToken.END_ARRAY && token!=JsonToken.END_OBJECT && ++values>1000000)throw new Problem("INVALID_JSON");
                    if(token.isNumeric()) {
                        BigDecimal exact=parser.getDecimalValue();double represented=parser.getDoubleValue();
                        if(Math.abs((long)exact.scale())>400 || !Double.isFinite(represented) || exact.compareTo(BigDecimal.valueOf(represented))!=0)throw new Problem("INVALID_JSON");
                    }
                }
            }
            try(var parser=MAPPER.createParser(text)){root=MAPPER.readTree(parser);require(parser.nextToken()==null);}
        }catch(Problem e){throw e;}catch(IOException e){throw new Problem("INVALID_JSON");}
        array(root,9,9);var result=new ArrayList<Candidate>();
        for(int i=0;i<9;i++)if(!root.get(i).isNull()){
            JsonNode source=root.get(i);int version=number(source,"version",0,Integer.MAX_VALUE);
            result.add(new Candidate(i,version,normalize(source,i)));
        }
        require(!result.isEmpty());return List.copyOf(result);
    }
    public static List<Candidate> read(Path path) throws IOException {
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new Problem("FILE_READ_FAILED");
        try(var channel=Files.newByteChannel(path,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS)) {
            if(channel.size()>MAX_BYTES)throw new Problem("REPLAY_TOO_LARGE");
            var data=new java.io.ByteArrayOutputStream();var buffer=ByteBuffer.allocate(8192);
            while(channel.read(buffer)>0){buffer.flip();data.write(buffer.array(),0,buffer.remaining());if(data.size()>MAX_BYTES)throw new Problem("REPLAY_TOO_LARGE");buffer.clear();}
            return parse(data.toByteArray());
        }
    }
    public static byte[] encode(List<ReplaySave> replays) throws IOException {
        require(!replays.isEmpty() && replays.size()<=9);var root=MAPPER.createArrayNode();for(int i=0;i<9;i++)root.addNull();
        for(int i=0;i<replays.size();i++){ObjectNode value=MAPPER.valueToTree(replays.get(i));value.put("slotIndex",i);root.set(i,value);}
        var normalized=MAPPER.createArrayNode();for(int i=0;i<9;i++)normalized.addNull();
        for(Candidate c:parse(MAPPER.writeValueAsBytes(root))){ObjectNode value=(ObjectNode)c.data();value.put("slotIndex",c.fileSlot());normalized.set(c.fileSlot(),value);}
        byte[] result=MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(normalized);
        if(result.length>MAX_BYTES)throw new Problem("REPLAY_TOO_LARGE");return result;
    }
    public static void write(Path path,byte[] bytes,boolean replace) throws IOException {
        parse(bytes);Path target=path.toAbsolutePath();
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS) && (!replace || !Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)))throw new Problem("FILE_EXISTS");
        Path temporary=Files.createTempFile(target.getParent(),".vocr-export-",".tmp");
        try {
            Files.write(temporary,bytes);
            if(replace)Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            else Files.move(temporary,target); // No replace: refuse a destination created after confirmation.
        }finally{Files.deleteIfExists(temporary);}
    }
}
