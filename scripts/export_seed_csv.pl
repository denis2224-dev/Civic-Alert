#!/usr/bin/env perl
use strict;
use warnings;
use utf8;
use open ':std', ':encoding(UTF-8)';

my $seed_file = $ARGV[0] // 'civicalert_seed_data_requirements.txt';
my $out_dir = $ARGV[1] // 'db-import';

if (!-f $seed_file) {
    die "Seed file not found: $seed_file\n";
}

if (!-d $out_dir) {
    mkdir $out_dir or die "Cannot create output directory $out_dir: $!\n";
}

open my $seed_fh, '<', $seed_file or die "Cannot open $seed_file: $!\n";
open my $official_fh, '>', "$out_dir/official_info.csv" or die "Cannot write official_info.csv: $!\n";
open my $verified_fh, '>', "$out_dir/verified_claims.csv" or die "Cannot write verified_claims.csv: $!\n";

print $official_fh csv_line(qw(topic content source_name source_url language));
print $verified_fh csv_line(
    qw(claim_text normalized_claim category status correction_text official_source official_source_url language region published)
);

my $section = '';
while (my $line = <$seed_fh>) {
    chomp $line;
    $line =~ s/\r$//;

    if ($line =~ /^##\s*OFFICIAL_INFO\b/) {
        $section = 'official';
        next;
    }
    if ($line =~ /^##\s*RUMOR_PATTERNS\b/) {
        $section = 'rumor';
        next;
    }
    if ($line =~ /^##\s*VERIFIED_CLAIMS\b/) {
        $section = 'verified';
        next;
    }

    next if $line =~ /^\s*#/;
    next if $line =~ /^\s*$/;

    if ($section eq 'official') {
        my @f = split /\|/, $line, -1;
        next unless @f == 5;
        print $official_fh csv_line(@f);
        next;
    }

    if ($section eq 'verified') {
        my @f = split /\|/, $line, -1;
        next unless @f == 9;

        my ($claim_text, $category, $status, $correction_text, $official_source, $official_source_url, $language, $region, $published) = @f;
        my $normalized_claim = normalize_text($claim_text);

        print $verified_fh csv_line(
            $claim_text,
            $normalized_claim,
            $category,
            $status,
            $correction_text,
            $official_source,
            $official_source_url,
            $language,
            $region,
            $published
        );
    }
}

close $seed_fh;
close $official_fh;
close $verified_fh;

sub csv_line {
    my @fields = @_;
    my @quoted = map {
        my $v = defined $_ ? $_ : '';
        $v =~ s/"/""/g;
        qq("$v");
    } @fields;
    return join(',', @quoted) . "\n";
}

sub normalize_text {
    my ($text) = @_;
    return '' if !defined $text;

    my $value = lc($text);
    $value =~ s/ă/a/g;
    $value =~ s/â/a/g;
    $value =~ s/î/i/g;
    $value =~ s/ș/s/g;
    $value =~ s/ş/s/g;
    $value =~ s/ț/t/g;
    $value =~ s/ţ/t/g;
    $value =~ s/Ă/a/g;
    $value =~ s/Â/a/g;
    $value =~ s/Î/i/g;
    $value =~ s/Ș/s/g;
    $value =~ s/Ş/s/g;
    $value =~ s/Ț/t/g;
    $value =~ s/Ţ/t/g;
    $value =~ s/[^\p{L}\p{N}\s]/ /g;
    $value =~ s/\s+/ /g;
    $value =~ s/^\s+|\s+$//g;
    return $value;
}
