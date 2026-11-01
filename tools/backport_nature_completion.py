"""Backport forest travel, moss and verified ambient/wolf sound assets."""
import copy,hashlib,json,urllib.request,zipfile,time
from concurrent.futures import ThreadPoolExecutor
from backport_forest_resources import ROOT,RES,write,remap,tag,recipe
from official_backport_lang import apply_official_chinese
from legacy_sign_textures import repack

def main():
    keys={}
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
                if 'door' in rel or 'moss_carpet' in rel: data['render_type']='minecraft:cutout'
                write(RES/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data


        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        for wood in ('pale_oak','poplar'):
            for part in ('sign','wall_sign','hanging_sign','wall_hanging_sign'):
                name=wood+'_'+part
                data=remap(json.loads(old.read(f'assets/minecraft/blockstates/oak_{part}.json')),{'minecraft:block/oak_'+('hanging_sign' if 'hanging' in part else 'sign'):'farmerstales:block/'+wood+'_'+('hanging_sign' if 'hanging' in part else 'sign')})
                write(RES/f'assets/farmerstales/blockstates/{name}.json',data)
                if not part.startswith('wall'):
                    model('minecraft:item/'+name)
                    write(RES/f'assets/farmerstales/models/block/{name}.json',{'textures':{'particle':f'farmerstales:block/{wood}_planks'}})
                    raw=remap(json.loads(old.read(f'data/minecraft/recipes/oak_{part}.json')),{'minecraft:oak_'+part:'farmerstales:'+name,'minecraft:oak_planks':'farmerstales:'+wood+'_planks','minecraft:stripped_oak_log':'farmerstales:stripped_'+wood+'_log'})
                    recipe(name,raw,'farmerstales:'+wood+'_planks')
                    keys['block.minecraft.'+name]='block.farmerstales.'+name
                loot=remap(json.loads(old.read('data/minecraft/loot_tables/blocks/oak_'+part.removeprefix('wall_')+'.json')),{'minecraft:oak_sign':'farmerstales:'+wood+'_sign','minecraft:oak_hanging_sign':'farmerstales:'+wood+'_hanging_sign'})
                write(RES/f'data/farmerstales/loot_tables/blocks/{name}.json',loot)
                for kind in ('blocks',):tag('minecraft',kind,{'sign':'standing_signs','wall_sign':'wall_signs','hanging_sign':'ceiling_hanging_signs','wall_hanging_sign':'wall_hanging_signs'}[part],['farmerstales:'+name])
                tag('minecraft','blocks','mineable/axe',['farmerstales:'+name])
            for hanging in (False,True):
                suffix='hanging_sign' if hanging else 'sign'
                oldpath='assets/minecraft/textures/entity/signs/'+('hanging/' if hanging else '')+'spruce.png'
                reference=jar.read('assets/minecraft/textures/block/spruce_'+suffix+'.png')
                target=jar.read('assets/minecraft/textures/block/'+wood+'_'+suffix+'.png')
                path=RES/('assets/farmerstales/textures/entity/signs/'+('hanging/' if hanging else '')+wood+'.png');path.parent.mkdir(parents=True,exist_ok=True)
                path.write_bytes(repack(old.read(oldpath),reference,target,hanging))
                gui='hanging_signs' if hanging else 'signs';texture('minecraft:gui/'+gui+'/'+wood)
                tag('minecraft','items','hanging_signs' if hanging else 'signs',['farmerstales:'+wood+'_'+suffix])
            for chest in (False,True):
                name=wood+('_chest_boat' if chest else '_boat');model('minecraft:item/'+name)
                texture('minecraft:entity/'+('chest_boat/' if chest else 'boat/')+wood)
                raw=remap(json.loads(old.read('data/minecraft/recipes/oak_'+('chest_boat' if chest else 'boat')+'.json')),{'minecraft:oak_boat':'farmerstales:'+wood+'_boat','minecraft:oak_chest_boat':'farmerstales:'+wood+'_chest_boat','minecraft:oak_planks':'farmerstales:'+wood+'_planks'})
                recipe(name,raw,'farmerstales:'+wood+'_planks')
                tag('minecraft','items','chest_boats' if chest else 'boats',['farmerstales:'+name]);keys['item.minecraft.'+name]='item.farmerstales.'+name
                if 'entity.minecraft.'+name in english:keys['entity.minecraft.'+name]='entity.farmerstales.'+name
        write(RES/'assets/minecraft/atlases/signs.json',{'sources':[{'type':'directory','source':'entity/signs','prefix':'entity/signs/'}]})
        write(RES/'assets/farmerstales/blockstates/pale_moss_carpet.json',state(json.loads(jar.read('assets/minecraft/blockstates/pale_moss_carpet.json'))))
        write(RES/'data/farmerstales/loot_tables/blocks/pale_moss_carpet.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:block_state_property','block':'farmerstales:pale_moss_carpet','properties':{'bottom':'true'}},{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'farmerstales:pale_moss_carpet'}]}]})
    official=json.loads((ROOT/'refer/minecraft-26.3-lang/sounds.json').read_text(encoding='utf-8'))
    names=['block.leaf_litter.'+s for s in ('break','step','place','hit','fall')]+['block.dry_grass.ambient','block.sand.idle','block.pale_hanging_moss.idle']+['block.eyeblossom.'+s for s in ('idle','open','close','open_long','close_long')]
    names+=['entity.wolf_'+v+'.'+s for v in ('angry','big','cute','grumpy','puglin','sad') for s in ('ambient','death','growl','hurt','pant','whine')]
    events={n:copy.deepcopy(official[n]) for n in names};index=json.loads((ROOT/'refer/minecraft-26.3-lang/asset-index.json').read_text(encoding='utf-8-sig'))['objects'];downloads={}
    for value in events.values():
        subtitle=value.get('subtitle')
        if subtitle and subtitle not in ('subtitles.block.generic.break','subtitles.block.generic.step','subtitles.block.generic.place','subtitles.block.generic.hit','subtitles.block.generic.fall'):
            target='subtitles.farmerstales.'+subtitle.removeprefix('subtitles.');keys[subtitle]=target;value['subtitle']=target
        for i,entry in enumerate(value['sounds']):
            asset=entry if isinstance(entry,str) else entry['name']
            if isinstance(entry,dict) and entry.get('type')=='event':continue
            path=RES/f'assets/farmerstales/sounds/{asset}.ogg';downloads[path]=index[f'minecraft/sounds/{asset}.ogg']['hash']
            if isinstance(entry,str):value['sounds'][i]='farmerstales:'+asset
            else:entry['name']='farmerstales:'+asset
    def download(task):
        path,sha=task
        if not path.exists() or hashlib.sha1(path.read_bytes()).hexdigest()!=sha:
            for attempt in range(4):
                try:
                    with urllib.request.urlopen(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',timeout=40) as response:data=response.read()
                    break
                except Exception:
                    if attempt==3:raise
                    time.sleep(1)
            assert hashlib.sha1(data).hexdigest()==sha;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(download,downloads.items()))
    path=RES/'assets/farmerstales/sounds.json';sounds=json.loads(path.read_text(encoding='utf-8'));sounds.update(events);write(path,sounds)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,value in data['locales'].items():
        raw=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'));value['translations'].update({k:raw[k] for k in keys})
    write(snapshot,data);apply_official_chinese((),extra_keys=keys)
    path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({v:english[k] for k,v in keys.items()});write(path,data)
    print('Generated signs, boats, complete moss shapes, official text and',len(downloads),'verified sound files.')
if __name__=='__main__':main()
