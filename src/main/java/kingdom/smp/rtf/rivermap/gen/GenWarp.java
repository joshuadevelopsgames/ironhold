package kingdom.smp.rtf.rivermap.gen;

import kingdom.smp.rtf.noise.domain.Domain;
import kingdom.smp.rtf.noise.domain.Domains;

public record GenWarp(Domain lake, Domain river) {
    public static final GenWarp EMPTY = new GenWarp(Domains.direct(), Domains.direct());
    
    public static GenWarp make(int seed, int continentScale) {
        Domain lake = Domains.domainPerlin(++seed, 200, 1, 300.0F);
        lake = Domains.add(lake, Domains.domainPerlin(++seed, 50, 2, 50.0F));

        Domain river = Domains.domainPerlin(++seed, 95, 1, 25.0F);
        river = Domains.add(river, Domains.domainPerlin(++seed, 16, 1, 5.0F));
        return new GenWarp(lake, river);
    }
}
