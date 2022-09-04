import os
from typing import List, Optional

import boto3
from botocore.exceptions import ClientError

from awscraper import ec2

if __name__ == '__main__':
    session = boto3.Session(profile_name='548094779648_ps-p-account-admin')
    sts_client = session.client("sts")
    caller = sts_client.get_caller_identity()
    print(f"Caller: user={caller.get('UserId')} account={caller.get('Account')}")

    # Note: session.get_available_regions("ec2") includes regions which are not enabled, so only
    #       using this to get the region to jump from.
    regions = session.get_available_regions("ec2")
    is_gov = regions[0].startswith("us-gov")

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
        if region != "us-east-1":
            continue

        region_session = boto3.Session(profile_name='548094779648_ps-p-account-admin', region_name=region)

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
        instance_map = ec2.gather(region_session)

        print(f"{region}: instances={len(instance_map)}")
        if instance_map:
            for k, v in instance_map.items():
                print(f"  {v}")
