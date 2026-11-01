"""Port tuff building resources and golden dandelion from the local official client."""
import copy, hashlib, json, urllib.request, zipfile
from backport_forest_resources import ROOT, RES, write, remap, tag, recipe
from official_backport_lang import apply_official_chinese
TUFF=('tuff_slab','tuff_stairs','tuff_wall','polished_tuff','polished_tuff_slab','polished_tuff_stairs','polished_tuff_wall','tuff_bricks','tuff_brick_slab','tuff_brick_stairs','tuff_brick_wall','chiseled_tuff','chiseled_tuff_bricks')
NAMES=TUFF+('golden_dandelion','potted_golden_dandelion')
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
                if 'dandelion' in rel: data['render_type']='minecraft:cutout'
                write(RES/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data


        for n in ('pause_mob_growth','reset_mob_growth'):
            write(RES/f'assets/farmerstales/particles/{n}.json',{'textures':['farmerstales:glint']})
        path=RES/'assets/farmerstales/textures/particle/glint.png';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(jar.read('assets/minecraft/textures/particle/glint.png'))
        mapping={'minecraft:'+n:'farmerstales:'+n for n in NAMES}
        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        for n in NAMES:
            write(RES/f'assets/farmerstales/blockstates/{n}.json',state(json.loads(jar.read(f'assets/minecraft/blockstates/{n}.json'))))
            if n.startswith('potted_'):
                template='potted_dandelion';tag('minecraft','blocks','flower_pots',['farmerstales:'+n])
            else:
                ref=json.loads(jar.read(f'assets/minecraft/items/{n}.json'))['model']['model'];target=model(ref)
                if ref!='minecraft:item/'+n:write(RES/f'assets/farmerstales/models/item/{n}.json',{'parent':target})
                template='stone_slab' if n.endswith('_slab') else 'dandelion' if n=='golden_dandelion' else 'tuff'
            loot=remap(json.loads(old.read(f'data/minecraft/loot_tables/blocks/{template}.json')),{'minecraft:'+template:'farmerstales:'+n,'minecraft:dandelion':'farmerstales:golden_dandelion'})
            write(RES/f'data/farmerstales/loot_tables/blocks/{n}.json',loot)
        for n in TUFF:
            tag('minecraft','blocks','mineable/pickaxe',['farmerstales:'+n])
            for suffix,group in (('_slab','slabs'),('_stairs','stairs'),('_wall','walls')):
                if n.endswith(suffix):
                    for kind in ('blocks','items'):tag('minecraft',kind,group,['farmerstales:'+n])
        for kind in ('blocks','items'):tag('minecraft',kind,'small_flowers',['farmerstales:golden_dandelion'])
        tag('minecraft','items','piglin_loved',['farmerstales:golden_dandelion'])
        tag('farmerstales','entity_types','cannot_be_age_locked',json.loads(jar.read('data/minecraft/tags/entity_type/cannot_be_age_locked.json'))['values'])
        for path in jar.namelist():
            if not path.startswith('data/minecraft/recipe/') or not path.endswith('.json'):continue
            n=path.split('/')[-1][:-5]
            if 'tuff' not in n and n not in ('golden_dandelion','yellow_dye_from_golden_dandelion'):continue
            data=remap(json.loads(jar.read(path)),mapping)
            def ingredient(v):
                if isinstance(v,list):return [ingredient(x) for x in v]
                return {'tag':v[1:]} if v.startswith('#') else {'item':v}
            if 'key' in data:data['key']={k:ingredient(v) for k,v in data['key'].items()}
            if 'ingredients' in data:data['ingredients']=[ingredient(v) for v in data['ingredients']]
            if 'ingredient' in data:data['ingredient']=ingredient(data['ingredient'])
            result=data['result'];result['item']=result.pop('id')
            if data['type']=='minecraft:stonecutting':data['result']=result['item'];data['count']=result.get('count',1)
            recipe(n,data,'farmerstales:golden_dandelion' if n=='yellow_dye_from_golden_dandelion' else 'minecraft:dandelion' if n=='golden_dandelion' else 'minecraft:tuff')
    source=json.loads((ROOT/'refer/minecraft-26.3-lang/sounds.json').read_text(encoding='utf-8'))
    index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects']
    events={k:copy.deepcopy(v) for k,v in source.items() if k.startswith(('item.golden_dandelion.','block.polished_tuff.','block.tuff_bricks.'))}
    for event in events.values():
        if 'subtitle' in event:
            key=event['subtitle'];target='subtitles.farmerstales.'+key.removeprefix('subtitles.');keys[key]=target;event['subtitle']=target
        for i,entry in enumerate(event['sounds']):
            asset=entry if isinstance(entry,str) else entry['name']
            if isinstance(entry,dict) and entry.get('type')=='event':continue
            path=RES/f'assets/farmerstales/sounds/{asset}.ogg';sha=index[f'minecraft/sounds/{asset}.ogg']['hash']
            if not path.exists() or hashlib.sha1(path.read_bytes()).hexdigest()!=sha:
                data=urllib.request.urlopen(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',timeout=40).read();assert hashlib.sha1(data).hexdigest()==sha
                path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
            if isinstance(entry,str):event['sounds'][i]='farmerstales:'+asset
            else:entry['name']='farmerstales:'+asset
    path=RES/'assets/farmerstales/sounds.json';data=json.loads(path.read_text());data.update(events);write(path,data)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,value in data['locales'].items():
        raw=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'));value['translations'].update({k:raw[k] for k in keys})
    write(snapshot,data);apply_official_chinese((),extra_keys=keys)
    path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({v:english[k] for k,v in keys.items()});write(path,data)
    print('Generated 13 tuff blocks, golden dandelion/pot, 40 recipes and official resources.')
if __name__=='__main__':main()
