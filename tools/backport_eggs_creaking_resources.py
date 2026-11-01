"""Backport verified local 26.3 egg/creaking assets and adapt data to Forge 1.20.1."""
import copy
import hashlib
import json
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor
from backport_forest_resources import ROOT, RES, write, remap, tag
from official_backport_lang import apply_official_chinese

BLOCKS = ('creaking_heart', 'resin_clump', 'resin_block', 'resin_bricks', 'resin_brick_stairs', 'resin_brick_slab', 'resin_brick_wall', 'chiseled_resin_bricks')
ITEMS = ('blue_egg', 'brown_egg', 'resin_brick', 'creaking_spawn_egg')

def main():
    mapping = {'minecraft:'+name:'farmerstales:'+name for name in BLOCKS+ITEMS+('pale_oak_log',)}
    with zipfile.ZipFile(ROOT/'refer/minecraft-26.3-client.jar') as jar, zipfile.ZipFile(ROOT/'build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar') as old:
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
                if rel=='block/resin_clump': data['render_type']='minecraft:cutout'
                write(RES/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data
        for name in BLOCKS:
            write(RES/f'assets/farmerstales/blockstates/{name}.json',state(json.loads(jar.read(f'assets/minecraft/blockstates/{name}.json'))))
        for name in BLOCKS+ITEMS:
            ref = json.loads(jar.read(f'assets/minecraft/items/{name}.json'))['model']['model']
            target = model(ref)
            if ref != 'minecraft:item/'+name:
                write(RES/f'assets/farmerstales/models/item/{name}.json', {'parent':target})
        for name in ('creaking','creaking_eyes'): texture('minecraft:entity/creaking/'+name)
        palette = RES/'assets/farmerstales/textures/trims/color_palettes/resin.png'
        palette.parent.mkdir(parents=True, exist_ok=True)
        palette.write_bytes(jar.read('assets/minecraft/textures/palettes/trim/resin.png'))
        # Old loot-table syntax and conditions; preserve six-face clump counts and double slabs.
        silk={'condition':'minecraft:match_tool','predicate':{'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}}
        for name in BLOCKS:
            if name=='resin_clump':
                loot=remap(json.loads(old.read('data/minecraft/loot_tables/blocks/glow_lichen.json')),{'minecraft:glow_lichen':'farmerstales:resin_clump'})
                # Resin is hand-harvestable, unlike glow lichen.
                for pool in loot['pools']:
                    pool.pop('conditions',None)
                    for entry in pool['entries']: entry.pop('conditions',None)
            elif name=='resin_brick_slab':
                loot=remap(json.loads(old.read('data/minecraft/loot_tables/blocks/brick_slab.json')),{'minecraft:brick_slab':'farmerstales:resin_brick_slab'})
            elif name=='creaking_heart':
                loot={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
                    {'type':'minecraft:item','name':'farmerstales:creaking_heart','conditions':[silk]},
                    {'type':'minecraft:item','name':'farmerstales:resin_clump','functions':[
                        {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}},
                        {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:uniform_bonus_count','parameters':{'bonusMultiplier':1}},
                        {'function':'minecraft:explosion_decay'}]}]}]}]}
            else: loot={'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'farmerstales:'+name}]}]}
            write(RES/f'data/farmerstales/loot_tables/blocks/{name}.json',loot)
        write(RES/'data/farmerstales/loot_tables/entities/creaking.json',{'type':'minecraft:entity','pools':[]})
        def ingredient(v):
            if isinstance(v,str): return {'tag':v[1:]} if v.startswith('#') else {'item':v}
            if isinstance(v,list): return [ingredient(x) for x in v]
            return v
        recipes=[n for n in jar.namelist() if n.startswith('data/minecraft/recipe/') and ('resin' in n or n.endswith('/creaking_heart.json'))]
        for source in recipes:
            data=remap(json.loads(jar.read(source)),mapping);data.pop('show_notification',None)
            if 'key' in data: data['key']={k:ingredient(v) for k,v in data['key'].items()}
            if 'ingredients' in data: data['ingredients']=[ingredient(v) for v in data['ingredients']]
            if 'ingredient' in data: data['ingredient']=ingredient(data['ingredient'])
            result=data['result'];kind=data['type']
            if kind in ('minecraft:smelting','minecraft:stonecutting'):
                data['result']=result['id'] if isinstance(result,dict) else result
                if kind=='minecraft:stonecutting': data['count']=result.get('count',1)
            elif isinstance(result,dict): result['item']=result.pop('id')
            write(RES/f'data/farmerstales/recipes/{source.split("/")[-1]}',data)
        for name in ('cake','pumpkin_pie'):
            data=json.loads(old.read(f'data/minecraft/recipes/{name}.json'))
            def replace_egg(v):
                if v=={'item':'minecraft:egg'}: return {'tag':'forge:eggs'}
                if isinstance(v,dict): return {k:replace_egg(x) for k,x in v.items()}
                if isinstance(v,list): return [replace_egg(x) for x in v]
                return v
            write(RES/f'data/minecraft/recipes/{name}.json',replace_egg(data))
        # Both existing 1.20.1 trim atlases can generate the new material from their own masks.
        for atlas in ('armor_trims','blocks'):
            source=json.loads(old.read(f'assets/minecraft/atlases/{atlas}.json'))
            sources=[]
            for entry in source['sources']:
                if entry['type'].removeprefix('minecraft:')=='paletted_permutations' and 'permutations' in entry:
                    entry['permutations']={'resin':'farmerstales:trims/color_palettes/resin'};sources.append(entry)
            write(RES/f'assets/minecraft/atlases/{atlas}.json',{'sources':sources})
        write(RES/'data/farmerstales/trim_material/resin.json',{'asset_name':'resin','ingredient':'farmerstales:resin_brick','item_model_index':1.1,'description':{'color':'#FC7812','translate':'trim_material.farmerstales.resin'}})
        for name in oldfiles:
            if not name.startswith('assets/minecraft/models/item/') or not name.endswith('.json'): continue
            data=json.loads(old.read(name))
            if not any('trim_type' in o.get('predicate',{}) for o in data.get('overrides',[])): continue
            item=name.split('/')[-1][:-5]
            armor=next((p for p in ('helmet','chestplate','leggings','boots') if item.endswith(p)),None)
            if armor is None: continue
            child=copy.deepcopy(data);child.pop('overrides',None)
            child['textures']['layer2' if item.startswith('leather_') else 'layer1']=f'minecraft:trims/items/{armor}_trim_resin'
            write(RES/f'assets/farmerstales/models/item/{item}_resin_trim.json',child)
            data['overrides'].append({'predicate':{'trim_type':1.1},'model':f'farmerstales:item/{item}_resin_trim'})
            write(RES/name,data)
        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        keys={f'block.minecraft.{n}':f'block.farmerstales.{n}' for n in BLOCKS}
        keys.update({f'item.minecraft.{n}':f'item.farmerstales.{n}' for n in ITEMS})
        keys.update({'entity.minecraft.creaking':'entity.farmerstales.creaking','trim_material.minecraft.resin':'trim_material.farmerstales.resin'})
    official=json.loads((ROOT/'refer/minecraft-26.3-lang/sounds.json').read_text(encoding='utf-8'))
    index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects']
    events={k:copy.deepcopy(v) for k,v in official.items() if k.startswith(('entity.creaking.','block.creaking_heart.','block.resin.','block.resin_bricks.'))}
    downloads={}
    for value in events.values():
        sub=value.get('subtitle')
        if sub and sub.startswith('subtitles.entity.creaking') or sub and sub.startswith('subtitles.block.creaking_heart'):
            target='subtitles.farmerstales.'+sub.removeprefix('subtitles.');keys[sub]=target;value['subtitle']=target
        for i,entry in enumerate(value['sounds']):
            asset=entry if isinstance(entry,str) else entry['name']
            path=RES/f'assets/farmerstales/sounds/{asset}.ogg'
            downloads[path]=index[f'minecraft/sounds/{asset}.ogg']['hash']
            if isinstance(entry,str): value['sounds'][i]='farmerstales:'+asset
            else: entry['name']='farmerstales:'+asset
    def download(task):
        path,sha=task
        if not path.exists() or hashlib.sha1(path.read_bytes()).hexdigest()!=sha:
            with urllib.request.urlopen(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',timeout=60) as response: data=response.read()
            assert hashlib.sha1(data).hexdigest()==sha
            path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    with ThreadPoolExecutor(max_workers=8) as pool: list(pool.map(download,downloads.items()))
    path=RES/'assets/farmerstales/sounds.json';sounds=json.loads(path.read_text());sounds.update(events);write(path,sounds)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,value in data['locales'].items():
        raw=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'))
        value['translations'].update({k:raw[k] for k in keys})
    write(snapshot,data);apply_official_chinese((),extra_keys=keys)
    path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({v:english[k] for k,v in keys.items()});write(path,data)
    for namespace in ('forge','c'): tag(namespace,'items','eggs',['minecraft:egg','farmerstales:blue_egg','farmerstales:brown_egg'])
    tag('minecraft','items','trim_materials',['farmerstales:resin_brick'])
    tag('minecraft','blocks','mineable/axe',['farmerstales:creaking_heart'])
    tag('minecraft','blocks','mineable/pickaxe',['farmerstales:'+n for n in BLOCKS if 'brick' in n])
    for kind in ('blocks','items'):
        for suffix,block in (('stairs','stairs'),('slabs','slab'),('walls','wall')):
            tag('minecraft',kind,suffix,['farmerstales:resin_brick_'+block])
    print(f'Generated {len(BLOCKS)} blocks, {len(ITEMS)} items, recipes, trims, official languages and {len(downloads)} verified sounds.')

if __name__=='__main__': main()
