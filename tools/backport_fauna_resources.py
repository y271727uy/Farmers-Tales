"""Build official potted plants, armadillo, wolf armor and spawn/sound data."""
import copy,hashlib,json,time,urllib.request,zipfile
from concurrent.futures import ThreadPoolExecutor
from backport_forest_resources import ROOT,RES,write,remap,tag,recipe
from official_backport_lang import apply_official_chinese

def main():
    keys={};spawns={}
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
                if 'door' in rel or 'moss_carpet' in rel or 'potted_' in rel: data['render_type']='minecraft:cutout'
                write((ROOT/"src/generated/resources" if rel in ("item/armadillo_scute","item/armadillo_spawn_egg") else RES)/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data



        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        for plant in ('pale_oak_sapling','poplar_sapling','open_eyeblossom','closed_eyeblossom'):
            name='potted_'+plant
            write(RES/f'assets/farmerstales/blockstates/{name}.json',state(json.loads(jar.read(f'assets/minecraft/blockstates/{name}.json'))))
            # New potted models may emit light through the old Forge model extension.
            loot={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:flower_pot'}],'conditions':[{'condition':'minecraft:survives_explosion'}]},{'rolls':1,'entries':[{'type':'minecraft:item','name':'farmerstales:'+plant}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
            write(RES/f'data/farmerstales/loot_tables/blocks/{name}.json',loot)
            tag('minecraft','blocks','flower_pots',['farmerstales:'+name]);keys['block.minecraft.'+name]='block.farmerstales.'+name
        for item in ('armadillo_scute','armadillo_spawn_egg','wolf_armor'):
            model('minecraft:item/'+item);keys['item.minecraft.'+item]='item.farmerstales.'+item
        keys['entity.minecraft.armadillo']='entity.farmerstales.armadillo'
        for ref in ('entity/armadillo/armadillo','entity/equipment/wolf_body/armadillo_scute','entity/equipment/wolf_body/armadillo_scute_overlay'):
            texture('minecraft:'+ref)
        for crack in ('low','medium','high'):texture('minecraft:entity/wolf/wolf_armor_crackiness_'+crack)
        raw=json.loads(jar.read('data/minecraft/recipe/wolf_armor.json'));raw['key']={'X':{'item':'farmerstales:armadillo_scute'}};raw['result']={'item':'farmerstales:wolf_armor'}
        recipe('wolf_armor',raw,'farmerstales:armadillo_scute')
        write(ROOT/'src/generated/resources/data/farmerstales/loot_tables/entities/armadillo.json',{'type':'minecraft:entity','pools':[]})
        for name in jar.namelist():
            if name.startswith('data/minecraft/worldgen/biome/') and name.endswith('.json'):
                data=json.loads(jar.read(name));settings=data.get('attributes',{}).get('minecraft:gameplay/natural_mob_spawns',{}).get('argument',{})
                for entry in settings.get('spawns_by_category',{}).get('creature',[]):
                    if entry['type']!='minecraft:armadillo':continue
                    count=entry['count'];minimum=count.get('min_inclusive',1) if isinstance(count,dict) else count;maximum=count.get('max_inclusive',minimum) if isinstance(count,dict) else count
                    biome=name.split('/')[-1][:-5]
                    spawns[biome]=entry
        tag('farmerstales','worldgen/biome','has_armadillos',['minecraft:'+b for b in spawns])
        write(ROOT/'src/generated/resources/data/farmerstales/forge/biome_modifier/add_armadillo_spawns.json',{'type':'forge:add_spawns','biomes':'#farmerstales:has_armadillos','spawners':[{'type':'farmerstales:armadillo','weight':10,'minCount':2,'maxCount':3}]})
        bypass=json.loads(jar.read('data/minecraft/tags/damage_type/bypasses_wolf_armor.json'));bypass['replace']=False;write(RES/'data/farmerstales/tags/damage_type/bypasses_wolf_armor.json',bypass)
    source=json.loads((ROOT/'refer/minecraft-26.3-lang/sounds.json').read_text(encoding='utf-8'));index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects']
    events={k:copy.deepcopy(v) for k,v in source.items() if k.startswith(('entity.armadillo.','item.wolf_armor.')) or k in ('item.armor.equip_wolf','item.armor.unequip_wolf')};downloads={}
    for event in events.values():
        subtitle=event.get('subtitle')
        if subtitle:
            target='subtitles.farmerstales.'+subtitle.removeprefix('subtitles.');keys[subtitle]=target;event['subtitle']=target
        for i,entry in enumerate(event['sounds']):
            asset=entry if isinstance(entry,str) else entry['name']
            if isinstance(entry,dict) and entry.get('type')=='event':continue
            path=RES/f'assets/farmerstales/sounds/{asset}.ogg';downloads[path]=index[f'minecraft/sounds/{asset}.ogg']['hash']
            if isinstance(entry,str):event['sounds'][i]='farmerstales:'+asset
            else:entry['name']='farmerstales:'+asset
    def download(task):
        path,sha=task
        if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest()==sha:return
        for attempt in range(4):
            try:
                with urllib.request.urlopen(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',timeout=40) as response:data=response.read()
                assert hashlib.sha1(data).hexdigest()==sha;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data);return
            except Exception:
                if attempt==3:raise
                time.sleep(1)
    with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(download,downloads.items()))
    path=RES/'assets/farmerstales/sounds.json';sounds=json.loads(path.read_text(encoding='utf-8'));sounds.update(events);write(path,sounds)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,value in data['locales'].items():
        original=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'));value['translations'].update({k:original[k] for k in keys})
    write(snapshot,data);apply_official_chinese((),extra_keys=keys)
    path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({v:english[k] for k,v in keys.items()});write(path,data)
    write(ROOT/'build/armadillo-spawn-audit.json',spawns)
    print('Generated 4 potted plants, fauna/armor resources,',len(spawns),'spawn biomes and',len(downloads),'verified sounds.')
if __name__=='__main__':main()
