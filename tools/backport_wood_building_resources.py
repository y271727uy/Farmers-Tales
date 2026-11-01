"""Generate both building wood sets and the official dry-foliage colormap locally."""
import json
import zipfile
from backport_forest_resources import ROOT, RES, write, remap, tag, recipe
from official_backport_lang import apply_official_chinese
PARTS=('stairs','slab','fence','fence_gate','door','trapdoor','button','pressure_plate')
NAMES=tuple(tree+'_'+part for tree in ('pale_oak','poplar') for part in PARTS)
def main():
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
                if 'door' in rel: data['render_type']='minecraft:cutout'
                write(RES/f'assets/farmerstales/models/{rel}.json',data)
            return 'farmerstales:'+rel
        def state(data):
            if isinstance(data,list): return [state(v) for v in data]
            if isinstance(data,dict): return {k:model(v) if k=='model' else state(v) for k,v in data.items()}
            return data

        english=json.loads(jar.read('assets/minecraft/lang/en_us.json'))
        for name in NAMES:
            tree=next(t for t in ('pale_oak','poplar') if name.startswith(t+'_'));part=name[len(tree)+1:]
            write(RES/f'assets/farmerstales/blockstates/{name}.json',state(json.loads(jar.read(f'assets/minecraft/blockstates/{name}.json'))))
            ref=json.loads(jar.read(f'assets/minecraft/items/{name}.json'))['model']['model']
            target=model(ref)
            if ref!='minecraft:item/'+name:write(RES/f'assets/farmerstales/models/item/{name}.json',{'parent':target})
            mapping={'minecraft:oak_'+part:'farmerstales:'+name,'minecraft:oak_planks':'farmerstales:'+tree+'_planks'}
            loot=remap(json.loads(old.read(f'data/minecraft/loot_tables/blocks/oak_{part}.json')),mapping)
            write(RES/f'data/farmerstales/loot_tables/blocks/{name}.json',loot)
            data=remap(json.loads(old.read(f'data/minecraft/recipes/oak_{part}.json')),mapping)
            recipe(name,data,'farmerstales:'+tree+'_planks')
            for kind in ('blocks','items'):
                tag('minecraft',kind,{'stairs':'wooden_stairs','slab':'wooden_slabs','fence':'wooden_fences','fence_gate':'fence_gates','door':'wooden_doors','trapdoor':'wooden_trapdoors','button':'wooden_buttons','pressure_plate':'wooden_pressure_plates'}[part],['farmerstales:'+name])
            tag('minecraft','blocks','mineable/axe',['farmerstales:'+name])
            if part=='fence_gate':tag('forge','blocks','fence_gates/wooden',['farmerstales:'+name]);tag('forge','items','fence_gates/wooden',['farmerstales:'+name])
            if part=='fence':tag('forge','blocks','fences/wooden',['farmerstales:'+name]);tag('forge','items','fences/wooden',['farmerstales:'+name])
        path=RES/'assets/farmerstales/textures/colormap/dry_foliage.png';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(jar.read('assets/minecraft/textures/colormap/dry_foliage.png'))
        overrides={}
        for name in jar.namelist():
            if name.startswith('data/minecraft/worldgen/biome/') and name.endswith('.json'):
                data=json.loads(jar.read(name));color=data.get('effects',{}).get('dry_foliage_color')
                if color is not None:
                    biome=name.split('/')[-1][:-5];namespace='farmerstales' if biome in ('pale_garden','dappled_forest') else 'minecraft'
                    overrides[namespace+':'+biome]=int(color.lstrip('#'),16) if isinstance(color,str) else color
        write(RES/'assets/farmerstales/dry_foliage_overrides.json',overrides)
        path=ROOT/'src/generated/resources/assets/farmerstales/lang/en_us.json';data=json.loads(path.read_text(encoding='utf-8'));data.update({'block.farmerstales.'+n:english['block.minecraft.'+n] for n in NAMES});write(path,data)
    snapshot=ROOT/'tools/data/minecraft-26.3-official-zh.json';data=json.loads(snapshot.read_text(encoding='utf-8'))
    for locale,asset in data['locales'].items():
        raw=json.loads((ROOT/f'refer/minecraft-26.3-lang/{locale}.json').read_text(encoding='utf-8-sig'))
        asset['translations'].update({'block.minecraft.'+n:raw['block.minecraft.'+n] for n in NAMES})
    write(snapshot,data);apply_official_chinese(NAMES)
    print('Generated 16 building blocks, recipes/loot/tags/official text and dry-foliage resources.')
if __name__=='__main__':main()
