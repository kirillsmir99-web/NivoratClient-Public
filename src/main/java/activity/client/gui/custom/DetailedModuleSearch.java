package activity.client.gui.custom;

import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.ModuleManager;
import activity.client.gui.custom.api.modules.Category;
import activity.client.module.setting.EnumSetting;
import java.text.Normalizer;
import java.util.*;

public final class DetailedModuleSearch {
    private record Document(Module module,String text,List<String> titles,Set<String> words) {}
    private record Match(Module module,int score) {}
    private static final Map<Module,Document> CACHE=new IdentityHashMap<>();
    private static String lastQuery,lastLanguage;
    private static List<Module> lastPool=List.of(),lastResult=List.of();
    public static void invalidate(){CACHE.clear();lastQuery=null;lastPool=List.of();lastResult=List.of();}
    private static final String EN="qwertyuiop[]asdfghjkl;'zxcvbnm,.";
    private static final String RU="йцукенгшщзхъфывапролджэячсмитьбю";
    private DetailedModuleSearch() {}

    public static List<Module> search(String query) {
        return search(ModuleManager.get().getSearchModules(),query);
    }

    public static List<Module> search(List<Module> pool,String query) {
        String language=VisualText.language();
        if(query.equals(lastQuery)&&language.equals(lastLanguage)&&pool.equals(lastPool))return lastResult;
        if(!language.equals(lastLanguage))CACHE.clear();
        lastLanguage=language;
        LinkedHashSet<String> variants=new LinkedHashSet<>();
        variants.add(normalize(query));variants.add(normalize(layout(query,EN,RU)));variants.add(normalize(layout(query,RU,EN)));
        variants.remove("");
        if(variants.isEmpty()){lastQuery=query;lastPool=List.copyOf(pool);lastResult=lastPool;return lastResult;}
        if(!pool.equals(lastPool)){
            Set<Module> live=Collections.newSetFromMap(new IdentityHashMap<>());
            live.addAll(pool);
            CACHE.keySet().removeIf(m->!live.contains(m));
        }
        List<Match> matches=new ArrayList<>();
        for(Module module:pool){
            Document doc=CACHE.computeIfAbsent(module,DetailedModuleSearch::document);
            int score=0;for(String variant:variants)score=Math.max(score,score(doc,variant));
            if(score>0)matches.add(new Match(module,score));
        }
        matches.sort(Comparator.comparingInt(Match::score).reversed().thenComparing(m->m.module().getName()));
        lastQuery=query;lastPool=List.copyOf(pool);lastResult=matches.stream().map(Match::module).toList();return lastResult;
    }

    private static Document document(Module module){
        List<String> titles=new ArrayList<>();List<String> fields=new ArrayList<>();
        titles.add(module.getName());
        if(module.delegate!=null){
            var source=module.delegate;
            for(String language:List.of("ru","en")){
                titles.add(VisualText.moduleName(source,language));fields.add(VisualText.moduleDescription(source,language));
                for(var setting:source.getSettings()){
                    fields.add(VisualText.settingName(source,setting,language));fields.add(VisualText.settingDescription(source,setting,language));
                    if(setting instanceof EnumSetting select)for(String option:select.getOptions()){
                        fields.add(option);fields.add(VisualText.resolve(select.getOptionName(option),language));
                        fields.add(VisualText.resolve(select.getOptionTooltip(option),language));
                    }
                }
            }
            titles.addAll(source.getAliases());fields.add(source.getCategory().name());
        }else{
            titles.add(module.getDisplayName());fields.add(module.getDescription());
            for(var setting:module.getSettings()){
                fields.add(setting.getName());fields.add(setting.getDisplayName());fields.add(setting.getDescription());
                if(setting instanceof activity.client.gui.custom.api.modules.settings.impl.SelectSetting select)for(String option:select.getOptions()){
                    fields.add(option);fields.add(select.labelFor(option));
                }
            }
        }
        fields.addAll(titles);String raw=String.join(" ",fields);
        String text=normalize(raw+" "+transliterate(raw));
        return new Document(module,text,titles.stream().map(DetailedModuleSearch::normalize).toList(),new HashSet<>(List.of(text.split(" +"))));
    }

    private static int score(Document doc,String query){
        String compact=query.replace(" ","");
        for(String title:doc.titles())if(title.replace(" ","").equals(compact))return 500;
        int rank=0;
        for(String word:query.split(" +")){
            if(doc.text().contains(word))rank+=50;
            else if(word.length()>=4 && doc.words().stream().anyMatch(candidate->oneEdit(word,candidate)))rank+=15;
            else return 0;
        }
        for(String title:doc.titles())if(title.contains(query)||title.replace(" ","").contains(compact))rank+=150;
        return rank;
    }

    public static String normalize(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll(" +"," ");}
    private static String layout(String text,String from,String to){StringBuilder out=new StringBuilder();for(char c:text.toLowerCase(Locale.ROOT).toCharArray()){int i=from.indexOf(c);out.append(i>=0?to.charAt(i):c);}return out.toString();}
    private static String transliterate(String text){
        String letters="абвгдежзийклмнопрстуфхцчшщъыьэюя";
        String[] values={"a","b","v","g","d","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"};
        StringBuilder out=new StringBuilder();for(char c:text.toLowerCase(Locale.ROOT).replace('ё','е').toCharArray()){int i=letters.indexOf(c);out.append(i>=0?values[i]:c);}return out.toString();
    }
    private static boolean oneEdit(String a,String b){
        if(Math.abs(a.length()-b.length())>1)return false;
        int i=0,j=0,edits=0;
        while(i<a.length()&&j<b.length()){
            if(a.charAt(i)==b.charAt(j)){i++;j++;continue;}
            if(++edits>1)return false;
            if(a.length()==b.length()){if(i+1<a.length()&&j+1<b.length()&&a.charAt(i)==b.charAt(j+1)&&a.charAt(i+1)==b.charAt(j)){i+=2;j+=2;}else{i++;j++;}}
            else if(a.length()>b.length())i++;else j++;
        }
        return edits+(a.length()-i)+(b.length()-j)<=1;
    }
}
