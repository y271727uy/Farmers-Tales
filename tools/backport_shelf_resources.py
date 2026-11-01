"""Port all thirteen official shelves, recipes, models, sounds and Chinese text."""
import copy, hashlib, json, urllib.request, zipfile
from concurrent.futures import ThreadPoolExecutor
from backport_forest_resources import ROOT, RES, write, remap, tag, recipe
from official_backport_lang import apply_official_chinese
WOODS=('oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','bamboo','crimson','warped','pale_oak','poplar')
NAMES=tuple(w+'_shelf' for w in WOODS)
def main():
    keys={'block.minecraft.'+n:'block.farmerstales.'+n for n in NAMES}
    with zipfile.ZipFile(ROOT/'refer/minecraft-26.3-client.jar') as jar,zipfile.ZipFile(ROOT/'build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar') as old:
        oldfiles = set(old.namelist())
        copied = set()
        def texture(ref):
            if ref.startswith('#'): return ref
            rel = ref.removeprefix('minecraft:')
            source = f'assets/minecraft/textures/{rel}.png'
            if source in oldfiles: return 'minecraft:'+rel
            dest = RES/f'assets/farmerstales/textures/{rel}.png';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(jar.read(source))
            return 'farmerstales:'+rel
        def model(ref):
            rel=ref.removeprefix('minecraft:');source=f'assets/minecraft/models/{rel}.json'
            if source in oldfiles: return 'minecraft:'+rel
            if rel not in copied:
                copied.add(rel);data=json.loads(jar.read(source))
                if 'parent' in data: data['parent']=model(data['parent'])
                data['textures']={k:texture(v) for k,v in data.get('textures',{}).items()}
                for element in data.get('elements',[]):
                    element.pop('shade_direction_override',None)
                    emission=element.pop('light_emission',0)
                    if emission: element['forge_data']={'block_light':emission,'sky_light':emission,'ambient_occlusion':False}
                if 'shelf' in rel: data['render_type']='minecraft:cutout'
                write(RES/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data



        mapping={'minecraft:'+n:'farmerstales:'+n for n in NAMES}
        mapping.update({'minecraft:stripped_'+w+'_log':'farmerstales:stripped_'+w+'_log' for w in ('pale_oak','poplar')})
        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        for n in NAMES:
            write(RES/f'assets/farmerstales/blockstates/{n}.json',state(json.loads(jar.read(f'assets/minecraft/blockstates/{n}.json'))))
            ref=json.loads(jar.read(f'assets/minecraft/items/{n}.json'))['model']['model'];target=model(ref)
            write(RES/f'assets/farmerstales/models/item/{n}.json',{'parent':target})
            loot=remap(json.loads(old.read('data/minecraft/loot_tables/blocks/oak_planks.json')),{'minecraft:oak_planks':'farmerstales:'+n})
            write(RES/f'data/farmerstales/loot_tables/blocks/{n}.json',loot)
            data=remap(json.loads(jar.read(f'data/minecraft/recipe/{n}.json')),mapping)
            data['key']={k:{'item':v} for k,v in data['key'].items()};data['result']['item']=data['result'].pop('id')
            recipe(n,data,next(iter(data['key'].values()))['item'])
            tag('minecraft','blocks','mineable/axe',['farmerstales:'+n])
        for kind in ('blocks','items'):tag('farmerstales',kind,'wooden_shelves',['farmerstales:'+n for n in NAMES])
    source=json.loads((ROOT/'refer/minecraft-26.3-lang/sounds.json').read_text(encoding='utf-8'))
    index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects']
    events={k:copy.deepcopy(v) for k,v in source.items() if k.startswith('block.shelf.')};downloads={}
    for event in events.values():
        if 'subtitle' in event:
            key=event['subtitle'];target='subtitles.farmerstales.'+key.removeprefix('subtitles.');keys[key]=target;event['subtitle']=target
        for i,entry in enumerate(event['sounds']):
            asset=entry if isinstance(entry,str) else entry['name']
            if isinstance(entry,dict) and entry.get('type')=='event':continue
            path=RES/f'assets/farmerstales/sounds/{asset}.ogg';downloads[path]=index[f'minecraft/sounds/{asset}.ogg']['hash']
            if isinstance(entry,str):event['sounds'][i]='farmerstales:'+asset
            else:entry['name']='farmerstales:'+asset
    def download(task):
        path,sha=task
        if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest()==sha:return
        data=urllib.request.urlopen(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',timeout=40).read();assert hashlib.sha1(data).hexdigest()==sha
        path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(download,downloads.items()))
    path=RES/'assets/farmerstales/sounds.json';data=json.loads(path.read_text(encoding='utf-8'));data.update(events);write(path,data)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,value in data['locales'].items():
        raw=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'));value['translations'].update({k:raw[k] for k in keys})
    write(snapshot,data);apply_official_chinese((),extra_keys=keys)
    path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({v:english[k] for k,v in keys.items()});write(path,data)
    print(f'Generated {len(NAMES)} shelves and recipes, {len(events)} sound events ({len(downloads)} files), official Chinese text.')
if __name__=='__main__':main()
