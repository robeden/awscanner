from typing import Optional

import boto3
from botocore.exceptions import ClientError
import click

from awscraper import ec2, s3

from colorama import init, Fore


@click.command()
@click.option('--profile', required=True)
@click.option('--color', default=True)
def scan(profile: str, color: bool):
    init(autoreset=True, strip=not color)

    session = boto3.Session(profile_name=profile)
    sts_client = session.client("sts")
    caller = sts_client.get_caller_identity()
    print(f"Caller: user={caller.get('UserId')} account={caller.get('Account')} "
          f"arn={caller.get('Arn')}")
    is_gov = caller.get('Arn').startswith('arn:aws-us-gov')

    # Note: session.get_available_regions("ec2") includes regions which are not enabled
    test_region = "us-gov-east-1" if is_gov else "us-east-1"
    regions = [r["RegionName"]
               for r in session.client("ec2", region_name=test_region).describe_regions()["Regions"]
               if r["OptInStatus"] != "not-opted-in"]
    print(f"Regions: {regions}")

    region_session = None
    partition: Optional[str] = None   # aws, aws-cn, aws-us-gov, aws-iso, aws-iso-b
    for region in regions:
        # TODO: REMOVE!
        if not region.startswith("us-"):
            continue

        region_session = boto3.Session(profile_name=profile, region_name=region)

        regional_sts = region_session.client("sts")
        # The point of this call is check if access to the region is enabled
        try:
            regional_sts.get_caller_identity()
        except ClientError as e:
            print(f"Skipping region: {region}")
            continue

        if partition is None:
            partition = region_session.get_partition_for_region(region)
            print(f"Partition: {partition}")

        region_printed = False
        ec2_client = region_session.client("ec2")
        instances = list(ec2.gather_instances(region_session, ec2_client))
        ebs_volumes = list(ec2.gather_ebs(region_session, ec2_client))
        buckets = list(s3.gather(region_session))

        print(f"{region}: instances={len(instances)}  buckets={len(buckets)}")
        for i in instances:
            print(f"  {Fore.GREEN if i.is_running() else Fore.RED}{i}")
        for e in ebs_volumes:
            print(f"  {Fore.GREEN if e.attached_instances else Fore.RED}{e}")
        for bucket in buckets:
            print(f"  {bucket}")


if __name__ == '__main__':
    scan()
